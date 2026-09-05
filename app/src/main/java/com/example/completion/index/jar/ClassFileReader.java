package com.example.completion.index.jar;

import com.example.completion.core.SymbolKind;
import com.example.completion.core.SymbolOrigin;
import com.example.completion.symbol.Symbol;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * High-performance, zero-dependency Java/Kotlin .class bytecode parser.
 * Extracts classes, methods, fields, and Kotlin Metadata without using ClassLoader or Reflection.
 */
public class ClassFileReader {

    public static class ParsedClassInfo {
        public String className = "";
        public String superClassName = "";
        public List<String> interfaces = new ArrayList<>();
        public int accessFlags;
        public boolean isInterface;
        public boolean isEnum;
        public boolean hasKotlinMetadata;
        public List<Symbol> declaredMembers = new ArrayList<>();
    }

    public static ParsedClassInfo parse(InputStream in) throws Exception {
        DataInputStream dis = new DataInputStream(in);

        int magic = dis.readInt();
        if (magic != 0xCAFEBABE) {
            throw new IllegalArgumentException("Not a valid JVM class file (invalid magic: " + Integer.toHexString(magic) + ")");
        }

        int minorVersion = dis.readUnsignedShort();
        int majorVersion = dis.readUnsignedShort();

        int constantPoolCount = dis.readUnsignedShort();
        Object[] constantPool = new Object[constantPoolCount];
        int[] classRefNameIndices = new int[constantPoolCount];

        // Parse constant pool
        for (int i = 1; i < constantPoolCount; i++) {
            int tag = dis.readUnsignedByte();
            switch (tag) {
                case 1: // CONSTANT_Utf8
                    constantPool[i] = dis.readUTF();
                    break;
                case 3: // CONSTANT_Integer
                case 4: // CONSTANT_Float
                    dis.readInt();
                    break;
                case 5: // CONSTANT_Long
                case 6: // CONSTANT_Double
                    dis.readLong();
                    i++; // Long/Double take two entries
                    break;
                case 7: // CONSTANT_Class
                    classRefNameIndices[i] = dis.readUnsignedShort();
                    break;
                case 8: // CONSTANT_String
                case 16: // CONSTANT_MethodType
                case 19: // CONSTANT_Module
                case 20: // CONSTANT_Package
                    dis.readUnsignedShort();
                    break;
                case 9: // CONSTANT_Fieldref
                case 10: // CONSTANT_Methodref
                case 11: // CONSTANT_InterfaceMethodref
                case 12: // CONSTANT_NameAndType
                case 17: // CONSTANT_Dynamic
                case 18: // CONSTANT_InvokeDynamic
                    dis.readInt();
                    break;
                case 15: // CONSTANT_MethodHandle
                    dis.readByte();
                    dis.readUnsignedShort();
                    break;
                default:
                    break;
            }
        }

        ParsedClassInfo info = new ParsedClassInfo();
        info.accessFlags = dis.readUnsignedShort();
        info.isInterface = (info.accessFlags & 0x0200) != 0;
        info.isEnum = (info.accessFlags & 0x4000) != 0;

        int thisClassIdx = dis.readUnsignedShort();
        if (thisClassIdx > 0 && thisClassIdx < constantPoolCount) {
            int nameIdx = classRefNameIndices[thisClassIdx];
            if (nameIdx > 0 && nameIdx < constantPoolCount && constantPool[nameIdx] instanceof String) {
                info.className = ((String) constantPool[nameIdx]).replace('/', '.');
            }
        }

        int superClassIdx = dis.readUnsignedShort();
        if (superClassIdx > 0 && superClassIdx < constantPoolCount) {
            int nameIdx = classRefNameIndices[superClassIdx];
            if (nameIdx > 0 && nameIdx < constantPoolCount && constantPool[nameIdx] instanceof String) {
                info.superClassName = ((String) constantPool[nameIdx]).replace('/', '.');
            }
        }

        int interfacesCount = dis.readUnsignedShort();
        for (int i = 0; i < interfacesCount; i++) {
            int ifaceIdx = dis.readUnsignedShort();
            if (ifaceIdx > 0 && ifaceIdx < constantPoolCount) {
                int nameIdx = classRefNameIndices[ifaceIdx];
                if (nameIdx > 0 && nameIdx < constantPoolCount && constantPool[nameIdx] instanceof String) {
                    info.interfaces.add(((String) constantPool[nameIdx]).replace('/', '.'));
                }
            }
        }

        // Fields
        int fieldsCount = dis.readUnsignedShort();
        for (int i = 0; i < fieldsCount; i++) {
            int fieldAccess = dis.readUnsignedShort();
            int nameIdx = dis.readUnsignedShort();
            int descIdx = dis.readUnsignedShort();

            String fieldName = (nameIdx > 0 && nameIdx < constantPoolCount && constantPool[nameIdx] instanceof String) ? (String) constantPool[nameIdx] : "";
            String fieldDesc = (descIdx > 0 && descIdx < constantPoolCount && constantPool[descIdx] instanceof String) ? (String) constantPool[descIdx] : "";

            int attrCount = dis.readUnsignedShort();
            for (int a = 0; a < attrCount; a++) {
                dis.readUnsignedShort(); // attr name
                int attrLen = dis.readInt();
                dis.skipBytes(attrLen);
            }

            if (!fieldName.isEmpty() && !fieldName.contains("$")) {
                info.declaredMembers.add(Symbol.builder()
                        .name(fieldName)
                        .qualifiedName(info.className + "." + fieldName)
                        .kind(SymbolKind.PROPERTY)
                        .origin(SymbolOrigin.JAR)
                        .containerName(info.className)
                        .returnType(parseDescriptorType(fieldDesc))
                        .isStatic((fieldAccess & 0x0008) != 0)
                        .build());
            }
        }

        // Methods
        int methodsCount = dis.readUnsignedShort();
        for (int i = 0; i < methodsCount; i++) {
            int methodAccess = dis.readUnsignedShort();
            int nameIdx = dis.readUnsignedShort();
            int descIdx = dis.readUnsignedShort();

            String methodName = (nameIdx > 0 && nameIdx < constantPoolCount && constantPool[nameIdx] instanceof String) ? (String) constantPool[nameIdx] : "";
            String methodDesc = (descIdx > 0 && descIdx < constantPoolCount && constantPool[descIdx] instanceof String) ? (String) constantPool[descIdx] : "";

            int attrCount = dis.readUnsignedShort();
            for (int a = 0; a < attrCount; a++) {
                dis.readUnsignedShort(); // attr name
                int attrLen = dis.readInt();
                dis.skipBytes(attrLen);
            }

            if (!methodName.isEmpty() && !methodName.equals("<clinit>") && !methodName.contains("$")) {
                String returnType = parseMethodReturnType(methodDesc);
                List<String> params = parseMethodParameters(methodDesc);
                info.declaredMembers.add(Symbol.builder()
                        .name(methodName.equals("<init>") ? getSimpleName(info.className) : methodName)
                        .qualifiedName(info.className + "." + methodName)
                        .kind(methodName.equals("<init>") ? SymbolKind.CLASS : SymbolKind.FUNCTION)
                        .origin(SymbolOrigin.JAR)
                        .containerName(info.className)
                        .returnType(methodName.equals("<init>") ? info.className : returnType)
                        .parameters(params)
                        .isStatic((methodAccess & 0x0008) != 0)
                        .build());
            }
        }

        // Check class attributes for Kotlin Metadata
        int classAttrCount = dis.readUnsignedShort();
        for (int a = 0; a < classAttrCount; a++) {
            int attrNameIdx = dis.readUnsignedShort();
            int attrLen = dis.readInt();
            String attrName = (attrNameIdx > 0 && attrNameIdx < constantPoolCount && constantPool[attrNameIdx] instanceof String) ? (String) constantPool[attrNameIdx] : "";

            if ("RuntimeVisibleAnnotations".equals(attrName)) {
                // Peek for kotlin/Metadata in the constant pool
                for (int c = 1; c < constantPoolCount; c++) {
                    if (constantPool[c] instanceof String && ((String) constantPool[c]).contains("Lkotlin/Metadata;")) {
                        info.hasKotlinMetadata = true;
                        break;
                    }
                }
            }
            dis.skipBytes(attrLen);
        }

        return info;
    }

    private static String parseDescriptorType(String desc) {
        if (desc == null || desc.isEmpty()) return "Any";
        switch (desc.charAt(0)) {
            case 'V': return "Unit";
            case 'Z': return "Boolean";
            case 'B': return "Byte";
            case 'C': return "Char";
            case 'S': return "Short";
            case 'I': return "Int";
            case 'J': return "Long";
            case 'F': return "Float";
            case 'D': return "Double";
            case 'L': {
                int semi = desc.indexOf(';');
                if (semi > 1) {
                    String full = desc.substring(1, semi).replace('/', '.');
                    return getSimpleName(full);
                }
                return "Any";
            }
            case '[':
                return "Array<" + parseDescriptorType(desc.substring(1)) + ">";
            default:
                return "Any";
        }
    }

    private static String parseMethodReturnType(String desc) {
        if (desc == null) return "Unit";
        int closeParen = desc.indexOf(')');
        if (closeParen != -1 && closeParen + 1 < desc.length()) {
            return parseDescriptorType(desc.substring(closeParen + 1));
        }
        return "Unit";
    }

    private static List<String> parseMethodParameters(String desc) {
        if (desc == null || !desc.startsWith("(")) return Collections.emptyList();
        int closeParen = desc.indexOf(')');
        if (closeParen == -1) return Collections.emptyList();

        List<String> list = new ArrayList<>();
        int i = 1;
        while (i < closeParen) {
            char c = desc.charAt(i);
            if (c == 'L') {
                int semi = desc.indexOf(';', i);
                if (semi != -1) {
                    String full = desc.substring(i + 1, semi).replace('/', '.');
                    list.add(getSimpleName(full));
                    i = semi + 1;
                } else {
                    break;
                }
            } else if (c == '[') {
                i++;
                if (i < closeParen && desc.charAt(i) == 'L') {
                    int semi = desc.indexOf(';', i);
                    if (semi != -1) {
                        String full = desc.substring(i + 1, semi).replace('/', '.');
                        list.add("Array<" + getSimpleName(full) + ">");
                        i = semi + 1;
                    }
                } else if (i < closeParen) {
                    list.add("Array<" + parseDescriptorType(String.valueOf(desc.charAt(i))) + ">");
                    i++;
                }
            } else {
                list.add(parseDescriptorType(String.valueOf(c)));
                i++;
            }
        }
        return list;
    }

    public static String getSimpleName(String qualifiedName) {
        if (qualifiedName == null) return "";
        int dot = qualifiedName.lastIndexOf('.');
        return dot != -1 ? qualifiedName.substring(dot + 1) : qualifiedName;
    }
}
