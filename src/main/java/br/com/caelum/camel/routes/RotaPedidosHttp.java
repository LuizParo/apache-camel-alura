package br.com.caelum.camel.routes;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.impl.DefaultCamelContext;

public class RotaPedidosHttp {

	public static void main(String[] args) throws Exception {

		CamelContext context = new DefaultCamelContext();
		context.addRoutes(new RouteBuilder() {
            
            @Override
            public void configure() throws Exception {
                from("file:pedidos?delay=5s&noop=true")
                .routeId("rota-pedidos")
                    .multicast()
                        .parallelProcessing()
                            .timeout(500)
                                .to("direct:soap")
                                .to("direct:http");
                
                from("direct:http")
                    .routeId("rota-http")
                    .setProperty("pedidoId", this.xpath("/pedido/id/text()"))
                    .setProperty("clienteId", this.xpath("/pedido/pagamento/email-titular/text()"))
                    .split()
                        .xpath("/pedido/itens/item")
                    .filter()
                        .xpath("/item/formato[text() = 'EBOOK']")
                    .setProperty("ebookId", this.xpath("/item/livro/codigo/text()"))
                    .marshal().xmljson()
                    .log("${id} \n ${body}")
                .setHeader(Exchange.HTTP_METHOD, this.constant(HttpMethods.GET))
                .setHeader(Exchange.HTTP_QUERY, this.simple("clienteId=${property.clienteId}&pedidoId=${property.pedidoId}&ebookId=${property.ebookId}"))
                .to("http4://localhost:8080/webservices/ebook/item");
                
                from("direct:soap")
                        .routeId("rota-soap")
                    .to("xslt:pedido-para-soap.xslt")
                        .log("Resultado do Template: ${body}")
                    .setHeader(Exchange.CONTENT_TYPE, this.constant("text/xml"))
                .to("http4://localhost:8080/webservices/financeiro");
            }
        });
		
		context.start();
		Thread.sleep(10000);
		context.stop();
	}	
}