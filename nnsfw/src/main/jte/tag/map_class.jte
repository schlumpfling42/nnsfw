@import net.nnwsf.controller.documentation.model.MapClassDescription
@param MapClassDescription model
{
    <div style="margin-left: 10px">@tag.class(model.getKeyDescription()) : @tag.class(model.getValueDescription())</div>
}