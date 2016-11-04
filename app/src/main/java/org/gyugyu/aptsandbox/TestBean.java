package org.gyugyu.aptsandbox;

public class TestBean {
    static final String TEST_NAME = "foo";

    private String arg1;
    private int arg2;

    public TestBean(String arg1, int arg2) {
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Extra(TEST_NAME)
    public String getArg1() {
        return arg1;
    }
}
