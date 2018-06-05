import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toCollection;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import FCTPClient.data.Edge;
import FCTPClient.data.FCTPGraph;
import FCTPClient.data.Node;
import FCTPClient.utils.InstanceConverter;
import scpsolver.constraints.LinearBiggerThanEqualsConstraint;
import scpsolver.constraints.LinearSmallerThanEqualsConstraint;
import scpsolver.lpsolver.LinearProgramSolver;
import scpsolver.lpsolver.SolverFactory;
import scpsolver.problems.LinearProgram;




public class transporte{
	
	private static int[] demand;
    private static int[] supply;
    private static double[][] costs;
    private static Shipment[][] matrix;
	
    private static class Shipment {
        final double costPerUnit;
        final int r, c;
        double quantity;
 
        public Shipment(double q, double cpu, int r, int c) {
            quantity = q;
            costPerUnit = cpu;
            this.r = r;
            this.c = c;
        }
    }
    
	public static void main(String[] args) throws Exception {
		
		File folder = new File("/Users/beatrizsantiago/git/gametheory/FCTPClient/FCTPInstances");
		File[] listOfFiles = folder.listFiles();
		
		FCTPGraph problem;
		
		//String filename = ".\\Instancias\\" + listOfFiles[1].getName();
		
		//for (File filename : listOfFiles) {
			//System.out.println(filename.getName());
		//}
		
        //for (File filename : listOfFiles) {
        	//String fullname = "/Users/beatrizsantiago/git/gametheory/FCTPClient/FCTPInstances/" + filename.getName();
        	String fullname = "/Users/beatrizsantiago/git/gametheory/FCTPClient/FCTPInstances/" + listOfFiles[1].getName();
        	System.out.println(fullname);
        	problem = InstanceConverter.convert(fullname);
            System.out.println(problem.getName());
            init(problem);
            northWestCornerRule();
            steppingStone();
            printResult(fullname);
        //}
    }
	
	static void printResult(String filename) {
        System.out.printf("Optimal solution %s%n%n", filename);
        double totalCosts = 0;
 
        for (int r = 0; r < supply.length; r++) {
            for (int c = 0; c < demand.length; c++) {
 
                Shipment s = matrix[r][c];
                if (s != null && s.r == r && s.c == c) {
                    System.out.printf(" %3s ", (int) s.quantity);
                    totalCosts += (s.quantity * s.costPerUnit);
                } else
                    System.out.printf("  -  ");
            }
            System.out.println();
        }
        System.out.printf("%nTotal costs: %s%n%n", totalCosts);
    }

	private static void steppingStone() {
		// TODO Auto-generated method stub
		double maxReduction = 0;
        Shipment[] move = null;
        Shipment leaving = null;
 
        fixDegenerateCase();
 
        for (int r = 0; r < supply.length; r++) {
            for (int c = 0; c < demand.length; c++) {
 
                if (matrix[r][c] != null)
                    continue;
 
                Shipment trial = new Shipment(0, costs[r][c], r, c);
                Shipment[] path = getClosedPath(trial);
 
                double reduction = 0;
                double lowestQuantity = Integer.MAX_VALUE;
                Shipment leavingCandidate = null;
 
                boolean plus = true;
                for (Shipment s : path) {
                    if (plus) {
                        reduction += s.costPerUnit;
                    } else {
                        reduction -= s.costPerUnit;
                        if (s.quantity < lowestQuantity) {
                            leavingCandidate = s;
                            lowestQuantity = s.quantity;
                        }
                    }
                    plus = !plus;
                }
                if (reduction < maxReduction) {
                    move = path;
                    leaving = leavingCandidate;
                    maxReduction = reduction;
                }
            }
        }
 
        if (move != null) {
            double q = leaving.quantity;
            boolean plus = true;
            for (Shipment s : move) {
                s.quantity += plus ? q : -q;
                matrix[s.r][s.c] = s.quantity == 0 ? null : s;
                plus = !plus;
            }
            steppingStone();
        }
	}

	static void fixDegenerateCase() {
		// TODO Auto-generated method stub
		final double eps = Double.MIN_VALUE;
		 
        if (supply.length + demand.length - 1 != matrixToList().size()) {
 
            for (int r = 0; r < supply.length; r++)
                for (int c = 0; c < demand.length; c++) {
                    if (matrix[r][c] == null) {
                        Shipment dummy = new Shipment(eps, costs[r][c], r, c);
                        if (getClosedPath(dummy).length == 0) {
                            matrix[r][c] = dummy;
                            return;
                        }
                    }
                }
        }
	}

	static Shipment[] getClosedPath(Shipment s) {
        LinkedList<Shipment> path = matrixToList();
        path.addFirst(s);
 
        // remove (and keep removing) elements that do not have a
        // vertical AND horizontal neighbor
        while (path.removeIf(e -> {
            Shipment[] nbrs = getNeighbors(e, path);
            return nbrs[0] == null || nbrs[1] == null;
        }));
 
        // place the remaining elements in the correct plus-minus order
        Shipment[] stones = path.toArray(new Shipment[path.size()]);
        Shipment prev = s;
        for (int i = 0; i < stones.length; i++) {
            stones[i] = prev;
            prev = getNeighbors(prev, path)[i % 2];
        }
        return stones;
    }

	static Shipment[] getNeighbors(Shipment s, LinkedList<Shipment> lst) {
        Shipment[] nbrs = new Shipment[2];
        for (Shipment o : lst) {
            if (o != s) {
                if (o.r == s.r && nbrs[0] == null)
                    nbrs[0] = o;
                else if (o.c == s.c && nbrs[1] == null)
                    nbrs[1] = o;
                if (nbrs[0] != null && nbrs[1] != null)
                    break;
            }
        }
        return nbrs;
    }

	private static LinkedList<Shipment> matrixToList() {
		return stream(matrix)
                .flatMap(row -> stream(row))
                .filter(s -> s != null)
                .collect(toCollection(LinkedList::new));
	}

	static void northWestCornerRule() {
		
		for (int r = 0, northwest = 0; r < supply.length; r++)
            for (int c = northwest; c < demand.length; c++) {
 
                int quantity = Math.min(supply[r], demand[c]);
                if (quantity > 0) {
                    matrix[r][c] = new Shipment(quantity, costs[r][c], r, c);
 
                    supply[r] -= quantity;
                    demand[c] -= quantity;
 
                    if (supply[r] == 0) {
                        northwest = c;
                        break;
                    }
                }
            }
	}

	private static void init(FCTPGraph problem) {
		Collection<Node> sources = problem.getSources();
		Collection<Node> destinations = problem.getSinks();
		
		int numSources = sources.size();
        int numDestinations = destinations.size();

        List<Integer> src = new ArrayList<>();
        List<Integer> dst = new ArrayList<>();
        
        
        int totalSrc = 0;
        int totalDst = 0;
        
        for (Node source : sources){
            src.add(source.getId());
            totalSrc += source.getAmount();
        }

        for (Node destination : destinations){
            dst.add(destination.getId());
            totalDst += destination.getAmount();
        }

        // fix imbalance
        if (totalSrc > totalDst){
        	System.out.println("fix imbalance source > destination");
            dst.add(totalSrc - totalDst);
        }
        else if (totalDst > totalSrc){
        	System.out.println("fix imbalance source < destination");
        	src.add(totalDst - totalSrc);
        }
            

        supply = src.stream().mapToInt(i -> i).toArray();
        demand = dst.stream().mapToInt(i -> i).toArray();

        costs = new double[supply.length][demand.length];
        matrix = new Shipment[supply.length][demand.length];

        for (int i = 0; i < numSources; i++)
            for (int j = 0; j < numDestinations; j++)
            	costs[i][j] = problem.getEdge(supply[i], demand[j]).getC();
            	

	
	}

	
}