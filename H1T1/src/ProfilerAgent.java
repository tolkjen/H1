import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.proto.*;
import jade.domain.*;

@SuppressWarnings("serial")
public class ProfilerAgent extends Agent {
	// State names
	private static final String STATE_GET_GUIDE = "Get tour guide";
	private static final String STATE_GET_DETAILS = "Get tour details";
	private static final String STATE_LAST = "Finish";
	
	// Agent IDs
	private AID aidTourGuide = new AID("TourGuide", AID.ISLOCALNAME);
	private AID aidCurator = new AID("Curator", AID.ISLOCALNAME);
	
	protected void setup() {
		System.out.println("Profiler: begin operation");
		
		ACLMessage initTourRequest = new ACLMessage(ACLMessage.REQUEST);
		initTourRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		initTourRequest.setContent("request-tour-guide");
		initTourRequest.addReceiver(aidTourGuide);
		TourGuideInitiator initGuide = new TourGuideInitiator(this, initTourRequest);
		
		ACLMessage detailTourRequest = new ACLMessage(ACLMessage.REQUEST);
		detailTourRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		detailTourRequest.setContent("request-tour-details");
		detailTourRequest.addReceiver(aidCurator);
		CuratorInitiator initCurator = new CuratorInitiator(this, detailTourRequest);
		
		OneShotBehaviour lastState = new OneShotBehaviour(this) {
			public void action() {}
		};
		
		FSMBehaviour fsm = new FSMBehaviour(this);
		
		fsm.registerFirstState(initGuide, STATE_GET_GUIDE);
		fsm.registerState(initCurator, STATE_GET_DETAILS);
		fsm.registerLastState(lastState, STATE_LAST);
		
		fsm.registerTransition(STATE_GET_GUIDE, STATE_GET_DETAILS, 0);
		fsm.registerTransition(STATE_GET_GUIDE, STATE_LAST, 1);
		fsm.registerDefaultTransition(STATE_GET_DETAILS, STATE_LAST);
		
		SequentialBehaviour seqBeh = new SequentialBehaviour(this);
		seqBeh.addSubBehaviour(fsm);
		seqBeh.addSubBehaviour(new OneShotBehaviour(this) {
			public void action() {
				System.out.println("Profiler: closing");
			}
			public int onEnd() {
				myAgent.doDelete();
				return super.onEnd();
			}
		});
		addBehaviour(seqBeh);
	} 
	
	private class TourGuideInitiator extends AchieveREInitiator {
		private Integer exitCode = 0;
		
		public TourGuideInitiator(Agent a, ACLMessage msg) {
			super(a, msg);
		}
		
		protected void handleInform(ACLMessage inform) {
			String text = inform.getContent();
			System.out.println("Profiler: response from Tour Guide ("+text+")");
			exitCode = 0;
		}
		
		protected void handleFailure(ACLMessage failure) {
			String text = failure.getContent();
			System.out.println("Profiler: Tour Guide failure ("+text+")");
			exitCode = 1;
		}
		
		public int onEnd() {
			return exitCode;
		}
	}
	
	private class CuratorInitiator extends AchieveREInitiator {
		private Integer exitCode = 0;
		
		public CuratorInitiator(Agent a, ACLMessage msg) {
			super(a, msg);
		}
		
		protected void handleInform(ACLMessage inform) {
			String text = inform.getContent();
			System.out.println("Profiler: response from Curator ("+text+")");
			exitCode = 0;
		}
		
		protected void handleFailure(ACLMessage failure) {
			String text = failure.getContent();
			System.out.println("Profiler: Curator failure ("+text+")");
			exitCode = 1;
		}
		
		public int onEnd() {
			return exitCode;
		}
	}
}
