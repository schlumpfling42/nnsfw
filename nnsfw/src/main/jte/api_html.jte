@import java.util.List
@import net.nnwsf.controller.documentation.model.ControllerDoc
@import net.nnwsf.controller.documentation.model.EndpointDoc
@param List<ControllerDoc> model

<!DOCTYPE html>
<html>
  <head>
    <title>API Documentation</title>
  </head>
    <body>
        @for(ControllerDoc aController : model)
          <div>
            <h2>${aController.getClassName()}</h2>
            <p>${aController.getDescription()}</p>
            <ul>
              @for(EndpointDoc anEndpoint : aController.getEndpoints())
                <li>
                  <h3>${anEndpoint.getMethod()}: ${anEndpoint.getPath()}</h3>
                  <p>${anEndpoint.getDescription()}</p>
                  <h4>Content-Type: ${anEndpoint.getContentType()}</h4>
                  @if((anEndpoint.getParameters() != null && !anEndpoint.getParameters().isEmpty()) || anEndpoint.getRequestBodyType() != null)
                  <h4>Request</h4>
                  <ul>
                    @if(anEndpoint.getParameters() != null && !anEndpoint.getParameters().isEmpty())
                      <li>Query parameters:
                        <ul>
                          @for(String aParameterKey : anEndpoint.getParameters().keySet())
                            <li>${aParameterKey}: ${anEndpoint.getParameters().get(aParameterKey)}</li>
                          @endfor
                        </ul>
                      </li>
                    @endif
                    @if(anEndpoint.getRequestBodyType() != null)
                      <li>Body: ${anEndpoint.getRequestBodyType().getName()}</li>
                    @endif
                  </ul>
                  @endif
                   @if(anEndpoint.getResponseBodyType() != null)
                    <h4>Response</h4>
                    <ul>
                      <li>Body: ${anEndpoint.getResponseBodyType().getName()}</li>
                    </ul>
                  @endif
                </li>
              @endfor
            </ul>
          </div>
        @endfor
    </body>
</html>