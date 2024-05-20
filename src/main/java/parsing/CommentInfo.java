package parsing;

class CommentInfo {
    private String comment;
    private int beginLineNumber;
    private int endLineNumber;
    private String methodName;
    private String fileName;

    public CommentInfo(String comment, int beginLineNumber, int endLineNumber, String methodName, String fileName) {
        this.comment = comment;
        this.beginLineNumber = beginLineNumber;
        this.endLineNumber = endLineNumber;
        this.methodName = methodName;
        this.fileName = fileName;
    }

    public String getComment() {
        return comment;
    }

    public int getBeginLineNumber() {
        return beginLineNumber;
    }

    public int getEndLineNumber() {
        return endLineNumber;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getFileName() {
        return fileName;
    }
}
