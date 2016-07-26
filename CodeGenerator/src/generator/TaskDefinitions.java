package generator;

public class TaskDefinitions
{
	public static final String[] names = new String[] {
			"epsilon.loadModel",
			"epsilon.eol",
			"epsilon.eunit",
			"epsilon.etl",
			"epsilon.ecl",
			"epsilon.eml",
			"epsilon.evl",
			"epsilon.egl",
			"epsilon.flock",
			"epsilon.storeModel",
			"epsilon.disposeModel",
			"epsilon.disposeModels",
			"epsilon.commitTransaction",
			"epsilon.startTransaction",
			"epsilon.rollbackTransaction",
			"epsilon.loadCompositeModel",
			"epsilon.xml.loadModel",
			"epsilon.epl",
			"epsilon.java.executeStaticMethod",
			"epsilon.emf.register",
	"epsilon.emf.loadModel"};

	public static final Class<?>[] classes = new Class[] {
			org.eclipse.epsilon.workflow.tasks.LoadModelTask.class,
			org.eclipse.epsilon.workflow.tasks.EolTask.class,
			org.eclipse.epsilon.workflow.tasks.EUnitTask.class,
			org.eclipse.epsilon.workflow.tasks.EtlTask.class,
			org.eclipse.epsilon.workflow.tasks.EclTask.class,
			org.eclipse.epsilon.workflow.tasks.EmlTask.class,
			org.eclipse.epsilon.workflow.tasks.EvlTask.class,
			org.eclipse.epsilon.workflow.tasks.EglTask.class,
			org.eclipse.epsilon.workflow.tasks.FlockTask.class,
			org.eclipse.epsilon.workflow.tasks.StoreModelTask.class,
			org.eclipse.epsilon.workflow.tasks.DisposeModelTask.class,
			org.eclipse.epsilon.workflow.tasks.DisposeModelsTask.class,
			org.eclipse.epsilon.workflow.tasks.transactions.CommitTransactionTask.class,
			org.eclipse.epsilon.workflow.tasks.transactions.StartTransactionTask.class,
			org.eclipse.epsilon.workflow.tasks.transactions.RollbackTransactionTask.class,
			org.eclipse.epsilon.workflow.tasks.LoadCompositeModelTask.class,
			org.eclipse.epsilon.workflow.tasks.xml.LoadXmlModel.class,
			org.eclipse.epsilon.workflow.tasks.EplTask.class,
			org.eclipse.epsilon.workflow.tasks.ExecuteStaticMethodTask.class,
			org.eclipse.epsilon.workflow.tasks.emf.RegisterTask.class,
			org.eclipse.epsilon.workflow.tasks.emf.LoadEmfModelTask.class};
}
