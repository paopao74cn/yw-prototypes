package org.yesworkflow.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.yesworkflow.comments.BeginComment;
import org.yesworkflow.comments.EndComment;
import org.yesworkflow.comments.InComment;
import org.yesworkflow.comments.OutComment;

public class Workflow extends Program {
	
	public final Program[] programs;
	public final Channel[] channels;
    
	public Workflow(
        BeginComment beginComment,
        EndComment endComment,
        List<Port> inPorts,
        List<Port> outPorts,
        List<Program> programs,
        List<Channel> channels
    ) {

	    super(beginComment, endComment, inPorts, outPorts);
		
    	this.programs = programs.toArray(new Program[programs.size()]);
    	this.channels = channels.toArray(new Channel[channels.size()]);
	}
	
	public static class Builder {
		
        private BeginComment beginComment;
        private EndComment endComment;
        private List<Port> inPorts = new LinkedList<Port>();
        private List<Port> outPorts = new LinkedList<Port>();

		private List<Program> nestedPrograms = new LinkedList<Program>();
        private List<Channel> nestedChannels = new LinkedList<Channel>();
		private Map<String,List<Port>> nestedInPorts = new LinkedHashMap<String,List<Port>>();
		private Map<String,Port> nestedOutPorts = new  LinkedHashMap<String,Port>();

        private Map<Port,String> programNameForPort = new HashMap<Port,String>();
        private Map<String,Program> programForName = new HashMap<String,Program>();

		public Builder begin(BeginComment comment) {
			this.beginComment = comment;
			return this;
		}

        public void end(EndComment comment) {
            this.endComment = comment;
        }

		public String getProgramName() {
			return beginComment.programName;
		}
		
		public Builder nestedProgram(Program program) {
			this.nestedPrograms.add(program);
			this.programForName.put(program.beginComment.programName, program);
			return this;
		}

        public Port inPort(InComment inComment) {
            Port port = new Port(inComment);
            inPorts.add(port);
            return port;
        }
        
        public Port outPort(OutComment outComment) {
            Port port = new Port(outComment);
            outPorts.add(port);
            return port;
        }
        
		public Builder nestedInPort(Port inPort, String programName) {
		    String binding = inPort.comment.binding();
			addNestedInport(binding, inPort);
			this.programNameForPort.put(inPort, programName);
			return this;
		}

		private void addNestedInport(String binding, Port inPort) {
			List<Port> ports = this.nestedInPorts.get(binding);
			if (ports == null) {
				ports = new LinkedList<Port>();
				this.nestedInPorts.put(binding, ports);
			}
			ports.add(inPort);
		}
		
		public Builder nestedOutPort(Port outPort, String programName) throws Exception {
			
			String binding = outPort.comment.binding();
			
			// ensure no other writers to this @out binding
			if (nestedOutPorts.containsKey(binding)) {
				throw new Exception("Multiple @out comments bound to " + binding);
			}
			
			// store the @out comment
			this.nestedOutPorts.put(binding, outPort);
			this.programNameForPort.put(outPort, programName);

			return this;
		}
		
		public Program build() throws Exception {
			
			// if no subprograms then we're building a simple program
			if (nestedPrograms.size() == 0) {				
				return new Program(beginComment, endComment, inPorts, outPorts);
			}
			
			// otherwise we're building a workflow and must build its channels
			for (Map.Entry<String, List<Port>> entry : nestedInPorts.entrySet()) {

				String binding = entry.getKey();
				List<Port> boundInPorts = entry.getValue();

				// get information about the @out port that writes to this binding
				Port boundOutPort = nestedOutPorts.get(binding);

				if (boundOutPort == null) {
				    //throw new Exception("No @out corresponding to @in " + binding);
					continue;
				}
				
			    String outProgramName = programNameForPort.get(boundOutPort);
				Program outProgram = programForName.get(outProgramName);
				
				// iterate over @in ports that bind to the current @out port
				for (Port inPort : boundInPorts) {
				
					// get information about this @in port
					String inProgramName = programNameForPort.get(inPort);
					Program inProgram = programForName.get(inProgramName);
	
					// store the new channel
					Channel channel = new Channel(outProgram, boundOutPort, inProgram, inPort);
					nestedChannels.add(channel);
				}		            
			}
			
			return new Workflow(
                beginComment,
                endComment,
                inPorts,
                outPorts,
				nestedPrograms,
				nestedChannels
			);
		}
	}
}
