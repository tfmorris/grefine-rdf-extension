package org.deri.grefine.rdf.operations;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import org.deri.grefine.rdf.RdfSchema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

public class SaveRdfSchemaOperation extends AbstractOperation {

    @JsonProperty("schema")
    final protected RdfSchema _schema;

    @JsonCreator
    public SaveRdfSchemaOperation(
                @JsonProperty("schema")
                RdfSchema schema) {
        this._schema = schema;
    }

    static public AbstractOperation reconstruct(Project project, JsonNode obj)
            throws Exception {
        return new SaveRdfSchemaOperation(RdfSchema.reconstruct(obj
                .get("schema")));
    }
    
    @JsonProperty("description")
    public String getDescription() {
    	return "Save RDF schema skeleton";
    }
    
    public RdfSchema getSchema() {
    	return _schema;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return "Save RDF schema skeleton";
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project,
            long historyEntryID) throws Exception {
        String description = "Save RDF schema skeleton";
        
        Change change = new RdfSchemaChange(_schema);
        
        return new HistoryEntry(historyEntryID, project, description,
                SaveRdfSchemaOperation.this, change);
    }

    static public class RdfSchemaChange implements Change {
        final protected RdfSchema _newSchema;
        protected RdfSchema _oldSchema;
        
        public RdfSchemaChange(RdfSchema schema) {
            _newSchema = schema;
        }
        
        public void apply(Project project) {
            synchronized (project) {
                _oldSchema = (RdfSchema) project.overlayModels.get("rdfSchema");
                project.overlayModels.put("rdfSchema", _newSchema);
            }
        }
        
        public void revert(Project project) {
            synchronized (project) {
                if (_oldSchema == null) {
                    project.overlayModels.remove("rdfSchema");
                } else {
                    project.overlayModels.put("rdfSchema", _oldSchema);
                }
            }
        }
        
        public void save(Writer writer, Properties options) throws IOException {
            writer.write("newSchema=");
            writeRdfSchema(_newSchema, writer);
            writer.write('\n');
            writer.write("oldSchema=");
            writeRdfSchema(_oldSchema, writer);
            writer.write('\n');
            writer.write("/ec/\n"); // end of change marker
        }
        
        static public Change load(LineNumberReader reader, Pool pool)
                throws Exception {
            RdfSchema oldSchema = null;
            RdfSchema newSchema = null;
            
            String line;
            while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
                int equal = line.indexOf('=');
                CharSequence field = line.subSequence(0, equal);
                String value = line.substring(equal + 1);
                
                if ("oldSchema".equals(field) && value.length() > 0) {
                    oldSchema = RdfSchema.reconstruct(ParsingUtilities
                            .evaluateJsonStringToObjectNode(value));
                } else if ("newSchema".equals(field) && value.length() > 0) {
                    newSchema = RdfSchema.reconstruct(ParsingUtilities
                            .evaluateJsonStringToObjectNode(value));
                }
            }
            
            RdfSchemaChange change = new RdfSchemaChange(newSchema);
            change._oldSchema = oldSchema;
            
            return change;
        }
        
        static protected void writeRdfSchema(RdfSchema s, Writer writer)
                throws IOException {
            if (s != null) {
                JsonGenerator jsonWriter = ParsingUtilities.mapper.getFactory().createGenerator(writer);
                try {
                    s.write(jsonWriter, new Properties());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
