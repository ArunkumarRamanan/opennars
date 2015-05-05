package nars.tuprolog.store;


import nars.tuprolog.ClauseInfo;
import nars.tuprolog.Struct;
import nars.tuprolog.SubGoalTree;

public class CollectionItemClause extends ClauseInfo
{
	private Object x;
	private SubGoalTree body = new SubGoalTree();

	public CollectionItemClause(Struct clause) 	{
        super(clause, null);
	//	this.x = x;
		this.clause = clause;
	}
	
	public SubGoalTree getBody()
	{
		return body;
	}

	public SubGoalTree getBodyCopy()
	{
		return body;
	}

	public Struct getClause()
	{
		return clause;
	}

	public Struct getHead()
	{
		return clause;
	}

	public Struct getHeadCopy()
	{
		return clause;
	}

	public String getLibName()
	{
		return null;
	}

	public void performCopy(int idExecCtx)
	{
		// nothing to do...
	}
}