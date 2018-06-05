import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import FCTPClient.data.Edge;
import FCTPClient.data.FCTPGraph;
import FCTPClient.data.Node;
import FCTPClient.utils.InstanceConverter;
import scpsolver.constraints.LinearBiggerThanEqualsConstraint;
import scpsolver.constraints.LinearEqualsConstraint;
import scpsolver.constraints.LinearSmallerThanEqualsConstraint;
import scpsolver.lpsolver.LinearProgramSolver;
import scpsolver.lpsolver.SolverFactory;
import scpsolver.problems.LinearProgram;
import scpsolver.util.SparseVector;

public class transp_lp_solve {
	
	
	private static int idx_mat2vec(int i, int j, int n, int m){
		//System.out.println("j: "+ (j-n));
		return (i-1)*m + j-n;
	}
	
	private static int[] idx_vec2mat(int k, int n, int m){
		int i = (k)/m +1;
		int j = (k) % m + n;
		return new int[] {i,j};
	}
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		File folder = new File("/Users/beatrizsantiago/git/gametheory/FCTPClient/FCTPInstances");
		File[] listOfFiles = folder.listFiles();
		
		FCTPGraph problem;
		
		String fullname = "/Users/beatrizsantiago/git/gametheory/FCTPClient/FCTPInstances/" + listOfFiles[1].getName();
    	//String fullname = "/Users/beatrizsantiago/Documents/workspace/FCTPClient/FCTPInstances/" + listOfFiles[1].getName();
    	System.out.println(fullname);
    	problem = InstanceConverter.convert(fullname);
		
		
		//funcao objetivo, usar cij
		
    	double[] custos = new double[problem.getEdges().size()];
    	int n = problem.getSources().size();
    	int m = problem.getSinks().size();
    	
    	int i=0;
    	int j = 0;
    	int k;
    	for (Edge edge : problem.getEdges()){
    		k = idx_mat2vec((edge.getSource()), edge.getSink(), n, m);
    		custos[k-1] = edge.getC();
    		i=i+1;
    		//System.out.println(" Source: " + edge.getSource() + " Sink: " +  edge.getSink());
    		//System.out.println(" k: " + k + " i: " + edge.getSource() + " j: " + (edge.getSink()-n));
    		//System.out.println("vet2mat: " + idx_vec2mat( k, n,  m)[0] + "-" + idx_vec2mat( k, n,  m)[1]);
    		//System.out.println("mat2vec: " + idx_mat2vec(edge.getSource(), (edge.getSink()),  n,  m));
    		
    	}
    	System.out.println("n: "+ n + " m: " + m);
    	
    	

		//LinearProgram lp = new LinearProgram(new double[]{5.0,10.0});
    	LinearProgram lp = new LinearProgram(custos);
    	
		for (Node source : problem.getSources()){
			double[] coef = new double[n*m];
			int s = source.getId();
			for(int a = m*(s-1)+1; a <= (s*m); a++){
				coef[a-1]=1;
			}
			lp.addConstraint(new LinearEqualsConstraint(coef, source.getAmount(), "source" + s));
		}
		
		for (Node sink : problem.getSinks()){
			double[] coef = new double[n*m];
			int s = sink.getId()-n;
			for(int a = s; a <= n*m; a+=m){
				coef[a-1]=1;
			}
			lp.addConstraint(new LinearEqualsConstraint(coef, sink.getAmount(), "sink" + s));
		}
		
		double[] lb = new double[n*m];
		
		lp.setLowerbound(lb);
		
		
		lp.setMinProblem(true); 
		LinearProgramSolver solver  = SolverFactory.newDefault(); 
		double[] sol = solver.solve(lp);
		
		System.out.println(calc_custo(sol,custos));

	}
	
	private static double calc_custo(double[] sol, double[] custos){
		double x = 0;
		for( int i = 0; i< sol.length; i++){
			x += sol[i]*custos[i];
		}
		return x;
	}


}
