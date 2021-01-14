package com.amadornes.rscircuits.api;

import com.amadornes.rscircuits.api.component.IComponentRegistry;

public class SCMAPI {

    private static final ISCMAPI instance;

    public static ISCMAPI getInstance() {

        return instance;
    }

    static {
        ISCMAPI inst = null;
        try {
            inst = (ISCMAPI) Class.forName("com.amadornes.rscircuits.SCMAPIImpl").newInstance();
        } catch (Exception e) {
            throw new IllegalStateException();
        }
        instance = inst;
    }

    public static interface ISCMAPI {

        public IComponentRegistry getComponentRegistry();

        public boolean shouldWiresOutputPower();

    }

}
