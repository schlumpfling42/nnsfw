@import net.nnwsf.controller.documentation.model.BeanClassDescription
@import net.nnwsf.controller.documentation.model.SimpleClassDescription
@import net.nnwsf.controller.documentation.model.CollectionClassDescription
@import net.nnwsf.controller.documentation.model.MapClassDescription
@param BeanClassDescription model
{
@for(String attributeName : model.getAttributeDescriptions().keySet())
    <div style="margin-left: 10px">"${attributeName}": @tag.class(model.getAttributeDescriptions().get(attributeName))</div>
@endfor
}