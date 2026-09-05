package com.example.completion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.completion.core.CompletionEngine;
import com.example.completion.core.CompletionItem;
import com.example.completion.core.CompletionLogger;
import com.example.completion.core.CompletionRequest;
import com.example.completion.core.CompletionResult;
import com.example.completion.core.CompletionScore;
import com.example.completion.core.SymbolKind;
import com.example.completion.core.SymbolOrigin;
import com.example.completion.index.jar.ClassFileReader;
import com.example.completion.index.jar.JarClassIndex;
import com.example.completion.index.jar.JarClassSymbol;
import com.example.completion.index.pkg.PackageIndex;
import com.example.completion.index.source.ProjectSymbol;
import com.example.completion.index.source.SourceSymbolIndex;
import com.example.completion.project.KotlinCompletionFacade;
import com.example.completion.psi.CompletionContextType;
import com.example.completion.psi.DefaultKotlinPsiParser;
import com.example.completion.psi.ParsedKotlinFile;
import com.example.completion.psi.PsiCompletionContext;
import com.example.completion.psi.PsiContextResolver;
import com.example.completion.resolver.LocalSymbolResolver;
import com.example.completion.resolver.MemberResolver;
import com.example.completion.resolver.SimpleTypeResolver;
import com.example.completion.resolver.TypeResolutionResult;
import com.example.completion.symbol.Symbol;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class KotlinCompletionEngineTest {

    private KotlinCompletionFacade facade;

    @Before
    public void setUp() {
        facade = new KotlinCompletionFacade.Builder()
                .logger(new CompletionLogger.NoOpLogger())
                .build();
    }

    @Test
    public void testCompletionScoreRanking() {
        int exactScore = CompletionScore.computeTextMatchScore("println", "println");
        int prefixScore = CompletionScore.computeTextMatchScore("println", "print");
        int fuzzyScore = CompletionScore.computeTextMatchScore("println", "prn");

        assertTrue("Exact match score must be highest", exactScore > prefixScore);
        assertTrue("Prefix match score must exceed fuzzy match", prefixScore > fuzzyScore);
        assertTrue("Fuzzy score must be positive", fuzzyScore > 0);
    }

    @Test
    public void testKotlinPsiParser() {
        String code = "package com.example.demo\n\n" +
                "class User(val id: Int, val name: String) {\n" +
                "    fun greet(): String = \"Hello\"\n" +
                "}\n\n" +
                "val globalCounter = 100\n";

        DefaultKotlinPsiParser parser = new DefaultKotlinPsiParser();
        ParsedKotlinFile parsed = parser.parse("Demo.kt", code);

        assertNotNull(parsed);
        assertEquals("com.example.demo", parsed.getPackageName());
        assertEquals(3, parsed.getTopLevelSymbols().size());

        boolean hasClass = false;
        boolean hasFunc = false;
        boolean hasProp = false;

        for (Symbol s : parsed.getTopLevelSymbols()) {
            if ("User".equals(s.getName()) && s.getKind() == SymbolKind.CLASS) hasClass = true;
            if ("greet".equals(s.getName()) && s.getKind() == SymbolKind.FUNCTION) hasFunc = true;
            if ("globalCounter".equals(s.getName()) && s.getKind() == SymbolKind.PROPERTY) hasProp = true;
        }

        assertTrue("Should extract User class", hasClass);
        assertTrue("Should extract greet function", hasFunc);
        assertTrue("Should extract globalCounter property", hasProp);
    }

    @Test
    public void testLocalSymbolResolver() {
        String code = "fun process() {\n" +
                "    val message = \"hello\"\n" +
                "    val count = 42\n" +
                "    val isDone = true\n" +
                "    m\n" +
                "}";

        int cursorOffset = code.indexOf("m\n") + 1;
        LocalSymbolResolver resolver = new LocalSymbolResolver();
        List<Symbol> localSymbols = resolver.resolveLocalSymbols(code, cursorOffset, null);

        assertFalse("Local symbols should not be empty", localSymbols.isEmpty());
        boolean hasMessage = false;
        boolean hasCount = false;

        for (Symbol s : localSymbols) {
            if ("message".equals(s.getName()) && "String".equals(s.getReturnType())) hasMessage = true;
            if ("count".equals(s.getName()) && "Int".equals(s.getReturnType())) hasCount = true;
        }

        assertTrue("Should resolve message: String", hasMessage);
        assertTrue("Should resolve count: Int", hasCount);
    }

    @Test
    public void testContextResolver() {
        PsiContextResolver resolver = new PsiContextResolver();

        // 1. Member access
        String code1 = "val user = User()\nuser.na";
        CompletionRequest req1 = CompletionRequest.create(code1, code1.length(), "Test.kt");
        PsiCompletionContext ctx1 = resolver.resolveContext(req1, null);
        assertEquals(CompletionContextType.MEMBER_ACCESS, ctx1.getContextType());
        assertEquals("user", ctx1.getReceiver());
        assertEquals("na", ctx1.getPrefix());

        // 2. Import statement
        String code2 = "import java.util.Array";
        CompletionRequest req2 = CompletionRequest.create(code2, code2.length(), "Test.kt");
        PsiCompletionContext ctx2 = resolver.resolveContext(req2, null);
        assertEquals(CompletionContextType.IMPORT, ctx2.getContextType());
        assertEquals("java.util.Array", ctx2.getPrefix());

        // 3. Package statement
        String code3 = "package com.example.a";
        CompletionRequest req3 = CompletionRequest.create(code3, code3.length(), "Test.kt");
        PsiCompletionContext ctx3 = resolver.resolveContext(req3, null);
        assertEquals(CompletionContextType.PACKAGE, ctx3.getContextType());
        assertEquals("com.example.a", ctx3.getPrefix());
    }

    @Test
    public void testMemberCompletion() {
        String code = "fun main() {\n" +
                "    val str = \"hello world\"\n" +
                "    str.len\n" +
                "}";

        int offset = code.indexOf("str.len") + 7;
        CompletionRequest request = CompletionRequest.create(code, offset, "Main.kt");
        CompletionResult result = facade.complete(request);

        assertNotNull(result);
        assertFalse("Should provide member completions for String", result.getItems().isEmpty());

        boolean foundLength = false;
        for (CompletionItem item : result.getItems()) {
            if ("length".equals(item.getLabel())) {
                foundLength = true;
                break;
            }
        }
        assertTrue("Should contain length property for String receiver", foundLength);
    }

    @Test
    public void testKeywordAndBuiltinCompletion() {
        String code = "pri";
        CompletionRequest request = CompletionRequest.create(code, 3, "Main.kt");
        CompletionResult result = facade.complete(request);

        assertNotNull(result);
        assertFalse("Should provide completions for 'pri'", result.getItems().isEmpty());

        boolean foundPrintln = false;
        boolean foundPrivate = false;

        for (CompletionItem item : result.getItems()) {
            if ("println".equals(item.getLabel())) foundPrintln = true;
            if ("private".equals(item.getLabel())) foundPrivate = true;
        }

        assertTrue("Should suggest println function", foundPrintln);
        assertTrue("Should suggest private keyword", foundPrivate);
    }

    @Test
    public void testPackageAndImportCompletion() {
        String code = "import java.util.Array";
        CompletionRequest request = CompletionRequest.create(code, code.length(), "Main.kt");
        CompletionResult result = facade.complete(request);

        assertNotNull(result);
        assertFalse("Should provide import completions", result.getItems().isEmpty());

        boolean foundArrayList = false;
        for (CompletionItem item : result.getItems()) {
            if ("ArrayList".equals(item.getLabel())) {
                foundArrayList = true;
                break;
            }
        }
        assertTrue("Should suggest ArrayList in java.util", foundArrayList);
    }
}
