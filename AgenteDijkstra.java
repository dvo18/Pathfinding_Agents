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

public class AgenteDijkstra extends AbstractPlayer {
	
	private static final int INF = Integer.MAX_VALUE;
	
	static class Nodo {
		int x, y;
		int coste;
		Nodo padre;
		ACTIONS act;
		
		public Nodo(int x, int y) {
			this.x = x; this.y = y;
			coste = INF; padre = null; act = ACTIONS.ACTION_NIL;
		}
		
		public Nodo(int x, int y, int coste) {
			this.x = x; this.y = y;
			this.coste = coste; padre = null; act = ACTIONS.ACTION_NIL;
		}
		
		public Nodo(int x, int y, int coste, ACTIONS act, Nodo padre) {
			this.x = x; this.y = y;
			this.coste = coste; this.padre = padre; this.act = act;
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
	HashSet<Nodo> cerrados;
	
	/**
	 * in
	 * @param so Observation of the current state.
	 * @param et Timer when the action turned is due.
	 */
	public AgenteDijkstra( StateObservation so, ElapsedCpuTimer et ) {
		// Calculamos el factor de escala entre mundos (pixeles -> grid)
		fescala = new Vector2d( so.getWorldDimension().width/so.getObservationGrid().length , so.getWorldDimension().height/so.getObservationGrid()[0].length );
		
		// De la lista de portales ordenada por cercanía al avatar, tomamos el más cercano, en nuestro caso solo existirá ese (una sola meta)
		portal = so.getPortalsPositions(so.getAvatarPosition())[0].get(0).position;
		portal.x = Math.floor(portal.x/fescala.x);
		portal.y = Math.floor(portal.y/fescala.y);
		
		acciones = new Stack<>();
		
		act = new ArrayList<Pair<ACTIONS,Pair<Integer,Integer>>>();
		act.add( new Pair<ACTIONS,Pair<Integer,Integer>>( ACTIONS.ACTION_UP, new Pair<Integer,Integer>(0,-1) ) );
		act.add( new Pair<ACTIONS,Pair<Integer,Integer>>( ACTIONS.ACTION_DOWN, new Pair<Integer,Integer>(0,1) ) );
		act.add( new Pair<ACTIONS,Pair<Integer,Integer>>( ACTIONS.ACTION_LEFT, new Pair<Integer,Integer>(-1,0) ) );
		act.add( new Pair<ACTIONS,Pair<Integer,Integer>>( ACTIONS.ACTION_RIGHT, new Pair<Integer,Integer>(1,0) ) );
		
		abiertos = new PriorityQueue<>(Comparator.comparingInt(n -> n.coste));
		cerrados = new HashSet<>();
	}
	
	
	/**
	 * 
	 * @param so
	 * @param et
	 */
	private void caminoDijkastra( StateObservation so ) {
		double tInicial = System.nanoTime();
		
		abiertos.add(new Nodo((int)avatar.x, (int)avatar.y, 0));
		
		while (!abiertos.isEmpty()) {
			Nodo n_actual = abiertos.poll();
			cerrados.add(n_actual);
			
			if (n_actual.x==(int)portal.x && n_actual.y==(int)portal.y) {
				while (n_actual.padre != null) {
					acciones.push(n_actual.act);
					n_actual = n_actual.padre;
				}
				break;
			}
			
			for ( Pair<ACTIONS,Pair<Integer,Integer>> a : act ) {
				if (so.getObservationGrid()[n_actual.x+a.second.first][n_actual.y+a.second.second].isEmpty() || (n_actual.x+a.second.first==(int)portal.x && n_actual.y+a.second.second==(int)portal.y) ) {
					Nodo n_vecino = new Nodo(n_actual.x+a.second.first, n_actual.y+a.second.second, n_actual.coste+1, a.first, n_actual);
					if (!cerrados.contains(n_vecino)) {
						boolean encontrado = false;
						
						for (Nodo n : abiertos) {
						    if (n.equals(n_vecino)) {
						        if (n_vecino.coste < n.coste) {
						            abiertos.remove(n);
						            abiertos.add(n_vecino);
						        }
						        encontrado = true;
						        break;
						    }
						}
						if (!encontrado) abiertos.add(n_vecino);
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

		if (acciones.isEmpty()) { this.caminoDijkastra(so); }
		
		if (!acciones.isEmpty()) { return acciones.pop(); }
		else { return Types.ACTIONS.ACTION_NIL; }
	}	
}