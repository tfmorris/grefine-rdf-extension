package org.deri.grefine.rdf.commands;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.deri.grefine.rdf.RdfSchema;
import org.deri.grefine.rdf.app.ApplicationContext;
import org.deri.grefine.rdf.operations.SaveRdfSchemaOperation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;

public class SaveRdfSchemaCommand extends RdfCommand{

    public SaveRdfSchemaCommand(ApplicationContext ctxt) {
		super(ctxt);
	}

	@Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if(!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        try {
            Project project = getProject(request);

            String jsonString = request.getParameter("schema");
            JsonNode json = ParsingUtilities.evaluateJsonStringToObjectNode(jsonString);
            RdfSchema schema = RdfSchema.reconstruct(json);

            AbstractOperation op = new SaveRdfSchemaOperation(schema);
            Process process = op.createProcess(project, new Properties());

            performProcessAndRespond(request, response, project, process);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
