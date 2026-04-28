package com.app.EMS.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.spring6.SpringTemplateEngine;

import org.thymeleaf.context.Context;
import java.io.ByteArrayOutputStream;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PdfGenerator {

    private final SpringTemplateEngine templateEngine;
    public byte[] generate(String template,Object data){
        Context context = new Context();
        context.setVariables(
                new ObjectMapper().convertValue(data, Map.class)
        );
        String html = templateEngine.process(template,context);
        try(ByteArrayOutputStream out = new ByteArrayOutputStream()){
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html,null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        }
        catch(Exception e){
            throw new RuntimeException("PDF generation failed",e);
        }
    }
}

