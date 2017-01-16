package neo4j.readWriteDB;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

public class Neo4JDBInterface
{
// <JANNIK> Made it public...
	public static GraphDatabaseService graphDb;
	public static Index<Node> nodeIndex;
// </JANNIK>

	static String databaseDir = "";

	static Transaction tx;

	public static void startTransaction()
	{
		tx = graphDb.beginTx();
	}

	public static void finishTransaction()
	{
		tx.success();
		tx.close();	
	}

	public static void setDatabaseDir(String aDir)
	{
		databaseDir = aDir;
	}

	public static void openDatabase()
	{

		Map<String, String> conf = ConfigurationGenerator
				.generateConfiguration();
		
		graphDb = new GraphDatabaseFactory()
				.newEmbeddedDatabaseBuilder(databaseDir).setConfig(conf)
				.newGraphDatabase();

		registerShutdownHook();
		startTransaction();
		
		nodeIndex = graphDb.index().forNodes("nodeIndex");
		
	}
	 private static void registerShutdownHook()
	 {
		 // Registers a shutdown hook for the Neo4j and index service instances
		 // so that it shuts down nicely when the VM exits (even if you
		 // "Ctrl-C" the running example before it's completed)
		 Runtime.getRuntime().addShutdownHook( new Thread()
		 {
			 @Override
			 public void run()
			 {
				 graphDb.shutdown();
			 }
		 } );
	 }
	
	
	
	public static IndexHits<Node> queryIndex(String query)
	{
		return nodeIndex.query(query);
	}

	public static void closeDatabase()
	{
		finishTransaction();
		graphDb.shutdown();
	}

	public static Node getNodeById(Long nodeId)
	{
		return graphDb.getNodeById(nodeId);
	}

	public static void removeEdge(long id)
	{
		graphDb.getRelationshipById(id).delete();
	}

	public static void addRelationship(long src, long dst,
			RelationshipType relType, Map<String, Object> properties, boolean check_for_duplicate)
	{
		Node node = graphDb.getNodeById(src);

//JANNIK: check, if that relationship already exists
		if(check_for_duplicate)
		{
			Iterable<Relationship> rels = node.getRelationships();
			for (Relationship rel : rels)
			{
				Node endNode = rel.getEndNode();
	
				if (endNode.getId() != dst)
					continue;
				if (!rel.getType().name().equals(relType.name()))
					continue;
	
				boolean all_equal = true;
				Iterable<String> keys = rel.getPropertyKeys();
				Iterator<String> iter = keys.iterator();
				while (iter.hasNext())
				{
					String key = iter.next();
					if(!rel.getProperty(key).equals(properties.get(key)))
					{
						all_equal = false;
						break;
					}
				}
	
				if(properties != null)
				{
					keys = properties.keySet();
					iter = keys.iterator();
					while (iter.hasNext())
					{
						String key = iter.next();
						if(!rel.getProperty(key).equals(properties.get(key)))
						{
							all_equal = false;
							break;
						}
					}
				}
				if (!all_equal)
					continue;
	
				return;
			}
		}
//System.out.println(String.valueOf(src) + " -> " + String.valueOf(dst) + " " + relType.name());

		Relationship rel = node.createRelationshipTo(graphDb.getNodeById(dst),
				relType);
		if (properties == null)
			return;
		for (Entry<String, Object> entry : properties.entrySet())
		{
			rel.setProperty(entry.getKey(), entry.getValue());
		}
	}

	public static void addRelationship(long src, long dst,
			RelationshipType relType, Map<String, Object> properties)
	{
		Neo4JDBInterface.addRelationship(src, dst, relType, properties, false);
	}
	
	public static Node addNode(Map<String,Object> properties)
	{
		Node newNode = graphDb.createNode();
		
		Set<Entry<String, Object>> entrySet = properties.entrySet();
		Iterator<Entry<String, Object>> it = entrySet.iterator();
		while(it.hasNext()){
			Entry<String, Object> next = it.next();
			newNode.setProperty(next.getKey(), next.getValue());
		}

		return newNode;
	}
	

}
