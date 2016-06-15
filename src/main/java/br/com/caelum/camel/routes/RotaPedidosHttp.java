package br.com.caelum.camel.routes;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.impl.DefaultCamelContext;

public class RotaPedidosHttp {

	public static void main(String[] args) throws Exception {

		CamelContext context = new DefaultCamelContext();
		context.addComponent("activemq", ActiveMQComponent.activeMQComponent("tcp://localhost:61616"));
		
		context.addRoutes(new RouteBuilder() {
            
            @Override
            public void configure() throws Exception {
                
                errorHandler(this.deadLetterChannel("activemq:queue:pedidos.DLQ")
                                .logExhaustedMessageHistory(true)
                                .maximumRedeliveries(3)
                                    .redeliveryDelay(3000)
                                .onRedelivery(new Processor() {            
                                            
                                    @Override
                                    public void process(Exchange exchange) throws Exception {
                                        int counter = (int) exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER);
                                        int max = (int) exchange.getIn().getHeader(Exchange.REDELIVERY_MAX_COUNTER);
                                        System.out.println("Redelivery - " + counter + "/" + max );
                                    }
                                }));
                
                from("activemq:queue:pedidos")
                    .routeId("rota-pedidos")
                    .to("validator:pedido.xsd")
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