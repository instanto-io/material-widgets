package io.instanto.material.build;

import com.github.javaparser.StaticJavaParser;

/** Reads native file metadata in its browser types instead of treating every property as text. */
final class UploadSourceAdapter {
  static String adapt(String source) {
    var cu = StaticJavaParser.parse(source);
    var type = cu.getType(0).asClassOrInterfaceDeclaration();
    var convert = type.getMethodsByName("convertUploadFile").get(0);
    if (!convert.toString().contains("Double.parseDouble(file.size)"))
      throw new IllegalArgumentException("Changed upload file conversion");
    convert.setBody(
        StaticJavaParser.parseBlock(
            """
        {
          if (file == null) return null;
          return new UploadFile(file.name, new Date((long) modifiedAt(file)), sizeOf(file), file.type);
        }
        """));
    type.addMember(
        StaticJavaParser.parseBodyDeclaration(
            """
        @org.teavm.jso.JSBody(params = "file", script = "return Number(file.size) || 0;")
        private static native double sizeOf(File file);
        """));
    type.addMember(
        StaticJavaParser.parseBodyDeclaration(
            """
        @org.teavm.jso.JSBody(params = "file", script = "var value=typeof file.lastModified==='number'?file.lastModified:new Date(file.lastModifiedDate).getTime(); return isFinite(value)?value:Date.now();")
        private static native double modifiedAt(File file);
        """));
    return cu.toString();
  }
}
