package io.airlift.airline.args.overrides;

import io.airlift.airline.Command;
import io.airlift.airline.Option;

@Command(name = "ArgsMergeTypeParent")
public class ArgsMergeTypeParent extends ArgsMergeParent {
    
    public static class A {
        public long value;
        
        public A(String value) {
            this.value = Long.parseLong(value);
        }
    }
    
    public static class B extends A {
        public B(String value) {
            super(value);
        }
    }
    
    public static class C extends B {
        public C(String value) {
            super(value);
        }
    }

    @Option(name = "--test", arity = 1)
    public B test;
    
    @Option(name = "--string", arity = 1)
    public String s;
}
