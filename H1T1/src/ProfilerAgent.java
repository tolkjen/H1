import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.proto.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

/**
 * Profiler agent class. Uses OneShotBehaviour, FSMBehaviour, SequentialBehaviour,
 * AcheiveREInitiator behavior.
 * 
 * @author tolkjen
 *
 */
@SuppressWarnings("serial")
public class ProfilerAgent extends Agent {
	/*
	 * This fields are names for states of Finite State Machine.
	 */
	private static final String STATE_GET_GUIDE = "Get tour guide";
	private static final String STATE_GET_DETAILS = "Get tour details";
	private static final String STATE_LAST = "Finish";

	/*
	 * Agent identifiers of curator and tour guide agents. They are temporary,
	 * since profiler agent will figure their IDs from DF.
	 */

	/**
	 * Prints a hello message and creates a sequential behavior. 
	 * 
	 * First sub-behavior is a finite state machine, which tries to get tour 
	 * information. It contacts tour guide agent to get the tour guide. In case 
	 * of failure, machine ends. If a response is received, it contacts curator 
	 * agent to get tour details.
	 * 
	 * Second sub-behavior prints goodbye message.
	 */
	protected void setup() {
		/*
		 * Hello message.
		 */
		System.out.println(this.getAID().getLocalName() + ": begin operation");

		/* 
		 * Request sent to the tour guide. It uses TourGuideInitiator class to
		 * make communication easier.
		 */
		ACLMessage initTourRequest = new ACLMessage(ACLMessage.REQUEST);
		initTourRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		initTourRequest.setContent("request-tour-guide");

		/*
		 * The Profiler looks for the registered Tour Guides.
		 */

		DFAgentDescription template = new DFAgentDescription(); 
		ServiceDescription sd = new ServiceDescription(); 
		sd.setType("tour-guide"); 
		template.addServices(sd);
		try { 
			DFAgentDescription[] result = DFService.search(this, template); 
			for (int i = 0; i < result.length; ++i) {
				initTourRequest.addReceiver(result[i].getName()); 
				}
		}   catch (FIPAException fe) {
			fe.printStackTrace();
			}

		TourGuideInitiator initGuide = new TourGuideInitiator(this, initTourRequest);

		/*
		 * Same as above, but with curator agent.
		 */
		ACLMessage detailTourRequest = new ACLMessage(ACLMessage.REQUEST);
		detailTourRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		detailTourRequest.setContent("request-tour-details");

		/*
		 * The Profiler looks for the registered curators.
		 */

		template = new DFAgentDescription(); 
		sd = new ServiceDescription(); 
		sd.setType("curator"); 
		template.addServices(sd);
		try { 
			DFAgentDescription[] result = DFService.search(this, template); 
			for (int i = 0; i < result.length; ++i) {
				detailTourRequest.addReceiver(result[i].getName()); 
				}
		}   catch (FIPAException fe) {
			fe.printStackTrace();
			}

		CuratorInitiator initCurator = new CuratorInitiator(this, detailTourRequest);

		/*
		 * State machine consists of two states:
		 * 1. Communication with tour guide agent
		 * 2. Communication with curator agent
		 * 3. Final state
		 * 
		 * It is constructed below.
		 */
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

		/*
		 * Sequential behavior is constructed and added as default agent 
		 * behavior.
		 */
		SequentialBehaviour seqBeh = new SequentialBehaviour(this);
		seqBeh.addSubBehaviour(fsm);
		seqBeh.addSubBehaviour(new OneShotBehaviour(this) {
			public void action() {
				System.out.println(myAgent.getAID().getLocalName() + ": closing");
			}
			public int onEnd() {
				myAgent.doDelete();
				return super.onEnd();
			}
		});
		//addBehaviour(seqBeh);
	} 

	/**
	 * This class performs 'talking' with Tour Guide agent. It responds to 
	 * a successful or faulty communication.
	 * 
	 * @author tolkjen
	 *
	 */
	private class TourGuideInitiator extends AchieveREInitiator {
		private Integer exitCode = 0;

		public TourGuideInitiator(Agent a, ACLMessage msg) {
			super(a, msg);
		}

		protected void handleInform(ACLMessage inform) {
			String text = inform.getContent();
			System.out.println(myAgent.getAID().getLocalName() + ": response from Tour Guide ("+text+")");
			exitCode = 0;
		}

		protected void handleFailure(ACLMessage failure) {
			String text = failure.getContent();
			System.out.println(myAgent.getAID().getLocalName() + ": Tour Guide failure ("+text+")");
			exitCode = 1;
		}

		public int onEnd() {
			return exitCode;
		}
	}

	/**
	 * This class performs 'talking' with Curator agent. It responds to 
	 * a successful or faulty communication.
	 * 
	 * @author tolkjen
	 *
	 */
	private class CuratorInitiator extends AchieveREInitiator {
		private Integer exitCode = 0;

		public CuratorInitiator(Agent a, ACLMessage msg) {
			super(a, msg);
		}

		protected void handleInform(ACLMessage inform) {
			String text = inform.getContent();
			System.out.println(myAgent.getAID().getLocalName() + ": response from Curator ("+text+")");
			exitCode = 0;
		}

		protected void handleFailure(ACLMessage failure) {
			String text = failure.getContent();
			System.out.println(myAgent.getAID().getLocalName() + ": Curator failure ("+text+")");
			exitCode = 1;
		}

		public int onEnd() {
			return exitCode;
		}
	}
}