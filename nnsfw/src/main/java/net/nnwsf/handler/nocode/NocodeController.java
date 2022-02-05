package net.nnwsf.handler.nocode;

import java.util.Collection;

import net.nnwsf.controller.documentation.annotation.ApiDoc;

@ApiDoc
public interface NocodeController<T> {
    @ApiDoc
    public Collection<T> get();
}
