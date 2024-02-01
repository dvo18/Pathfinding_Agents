package tracks.singlePlayer.evaluacion.src_VELAZQUEZ_ORTUÑO_DIEGO;

import java.util.*;
//import java.util.Arrays;
//import java.util.Comparator;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tools.Pair;

public class AgenteAStar extends AbstractPlayer {
	
	//private static final int INF = Integer.MAX_VALUE;
	
	static class Nodo {
		static int x_f, y_f;
		int x, y;
		int f, g, h;
		Nodo padre;
		ACTIONS act;
		
		static void setFinal (int x, int y) {
			x_f = x;
			y_f = y;
		}
		
		public Nodo(int x, int y) {
			this.x = x; this.y = y;
			g = 0; padre = null; act = ACTIONS.ACTION_NIL;
			this.calcularCoste();
		}
		
		public Nodo(int x, int y, int coste) {
			this.x = x; this.y = y;
			this.g = coste; padre = null; act = ACTIONS.ACTION_NIL;
			this.calcularCoste();
		}
		
		public Nodo(int x, int y, int coste, ACTIONS act, Nodo padre) {
			this.x = x; this.y = y;
			this.g = coste; this.padre = padre; this.act = act;
			this.calcularCoste();
		}
		
		private void calcularCoste() {
			h = Math.abs(x-x_f) + Math.abs(y-y_f);
			f = g + h;
		}
		
		@Override
		public int hashCode() { return Objects.hash(x, y); }

		@Override
		public boolean equals(Object obj) { Nodo n = (Nodo) obj; return this.x==n.x && this.y==n.y; }
	}
	
	Vector2d fescala, portal, avatar;
	
	ArrayList<Pair<ACTIONS,Pair<Integer,Integer>>> act;
	Stack<ACTIONS> acciones;
	
	PriorityQueue<Nodo> abiertos;
	HashMap<Integer,Nodo> cerrados;
	
	/**
	 * in
	 * @param so Observation of the current state.
	 * @param et Timer when the action turned is due.
	 */
	public AgenteAStar( StateObservation so, ElapsedCpuTimer et ) {
		// Calculamos el factor de escala entre mundos (pixeles -> grid)
		fescala = new Vector2d( so.getWorldDimension().width/so.getObservationGrid().length , so.getWorldDimension().height/so.getObservationGrid()[0].length );
		
		// De la lista de portales ordenada por cercanía al avatar, tomamos el más cercano, en nuestro caso solo existirá ese (una sola meta)
		portal = so.getPortalsPositions(so.getAvatarPosition())[0].get(0).position;
		portal.x = Math.floor(portal.x/fescala.x);
		portal.y = Math.floor(portal.y/fescala.y);
		
		Nodo.setFinal((int)portal.x,(int)portal.y);
		
		acciones = new Stack<>();
		
		act = new ArrayList<Pair<ACTIONS,Pair<Integer,Integer>>>();
		act.add( new Pair<ACTIONS,Pair<Integer,Integer>>( ACTIONS.ACTION_UP, new Pair<Integer,Integer>(0,-1) ) );
		act.add( new Pair<ACTIONS,Pair<Integer,Integer>>( ACTIONS.ACTION_DOWN, new Pair<Integer,Integer>(0,1) ) );
		act.add( new Pair<ACTIONS,Pair<Integer,Integer>>( ACTIONS.ACTION_LEFT, new Pair<Integer,Integer>(-1,0) ) );
		act.add( new Pair<ACTIONS,Pair<Integer,Integer>>( ACTIONS.ACTION_RIGHT, new Pair<Integer,Integer>(1,0) ) );
		
		abiertos = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
		cerrados = new HashMap<>();
	}
	
	
	/**
	 * 
	 * @param so
	 * @param et
	 */
	private void caminoAStar( StateObservation so ) {
		double tInicial = System.nanoTime();
		
		abiertos.add(new Nodo((int)avatar.x, (int)avatar.y, 0));
		
		while (!abiertos.isEmpty()) {
			Nodo n_actual = abiertos.poll();
			//cerrados.put(new Pair<>(n_actual.x,n_actual.y),n_actual);
			cerrados.put(n_actual.y*so.getObservationGrid().length+n_actual.x,n_actual);
			
			if (n_actual.x==(int)portal.x && n_actual.y==(int)portal.y) {
				while (n_actual.padre != null) {
					acciones.push(n_actual.act);
					n_actual = n_actual.padre;
				}
				break;
			}
			
			for ( Pair<ACTIONS,Pair<Integer,Integer>> a : act ) {
				if (so.getObservationGrid()[n_actual.x+a.second.first][n_actual.y+a.second.second].isEmpty() || (n_actual.x+a.second.first==(int)portal.x && n_actual.y+a.second.second==(int)portal.y) ) {
					Nodo n_vecino = new Nodo(n_actual.x+a.second.first, n_actual.y+a.second.second, n_actual.g+1, a.first, n_actual);
					
					if (cerrados.containsKey(n_vecino.y*so.getObservationGrid().length+n_vecino.x)) {
						if (n_vecino.g < cerrados.get(n_vecino.y*so.getObservationGrid().length+n_vecino.x).g) {
							cerrados.remove(n_vecino.y*so.getObservationGrid().length+n_vecino.x);
							abiertos.add(n_vecino);
						}
					}
					else if (!cerrados.containsKey(n_vecino.y*so.getObservationGrid().length+n_vecino.x) && !abiertos.contains(n_vecino)) {
						abiertos.add(n_vecino);
					}
					else if (abiertos.contains(n_vecino)) {
						for (Nodo n : abiertos) {
						    if (n.equals(n_vecino)) {
						        if (n_vecino.g < n.g) {
						            abiertos.remove(n);
						            abiertos.add(n_vecino);
						        }
						        break;
						    }
						}
					}
				}
			}
		}
		
		double tFinal = System.nanoTime();
		
		System.out.println("\nTº alg: " + (tFinal-tInicial) / 1000000 + "\tTam ruta: " + acciones.size() + "\tNº nodos expand: " + cerrados.size() );
	}
	
	
	/**
	 * return the best action to arrive faster to the closest portal
	 * @param os Observation of the current state.
     * @param et Timer when the action returned is due.
	 * @return best	ACTION to arrive faster to the closest portal
	 */
	@Override
	public ACTIONS act( StateObservation so, ElapsedCpuTimer et ) {
		avatar = new Vector2d( so.getAvatarPosition().x/fescala.x , so.getAvatarPosition().y/fescala.y );

		if (acciones.isEmpty()) { this.caminoAStar(so); }
		
		if (!acciones.isEmpty()) { return acciones.pop(); }
		else { return Types.ACTIONS.ACTION_NIL; }
	}	
}