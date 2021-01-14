package com.amadornes.rscircuits;

import com.amadornes.rscircuits.api.SCMAPI.ISCMAPI;
import com.amadornes.rscircuits.api.component.IComponentRegistry;
import com.amadornes.rscircuits.component.ComponentRegistry;
import com.amadornes.rscircuits.util.WireHelper;

public class SCMAPIImpl implements ISCMAPI {

    @Override
    public IComponentRegistry getComponentRegistry() {

        return ComponentRegistry.INSTANCE;
    }

    @Override
    public boolean shouldWiresOutputPower() {

        return WireHelper.wiresOutputPower;
    }

}
