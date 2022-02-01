@import net.nnwsf.controller.documentation.model.ClassDescription
@import net.nnwsf.controller.documentation.model.BeanClassDescription
@import net.nnwsf.controller.documentation.model.SimpleClassDescription
@import net.nnwsf.controller.documentation.model.CollectionClassDescription
@import net.nnwsf.controller.documentation.model.MapClassDescription
@param ClassDescription model
@if (model instanceof SimpleClassDescription)
${model.asString()}
@elseif (model instanceof CollectionClassDescription)
@tag.collection_class((CollectionClassDescription)model}
@elseif (model instanceof MapClassDescription)
@tag.map_class((MapClassDescription)model}
@elseif (model instanceof BeanClassDescription)
@tag.bean_class((BeanClassDescription)model)
@else
n/a
@endif