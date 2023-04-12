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


/*
public boolean equals(Object obj) {
	//if(obj == this) { return true; }
	//if(!(obj instanceof Nodo)) { return false; }
	Nodo n = (Nodo) obj;
	boolean prueba = this.x==n.x && this.y==n.y;
	System.out.println("Prueba comparacion: " + prueba);
	return prueba;
}*/

public class AgenteDijkstra extends AbstractPlayer {
	
	private static final int INF = Integer.MAX_VALUE;
	
	class Nodo {
		int x, y;
		int coste;
		Nodo padre;
		ACTIONS act;
		
		public Nodo(int x, int y) {
			this.x = x;
			this.y = y;
			coste = INF;
			padre = null;
			act = ACTIONS.ACTION_NIL;
		}
		
		public Nodo(int x, int y, int coste) {
			this.x = x;
			this.y = y;
			this.coste = coste;
			padre = null;
			act = ACTIONS.ACTION_NIL;
		}
		
		public Nodo(int x, int y, int coste, ACTIONS act, Nodo padre) {
			this.x = x;
			this.y = y;
			this.coste = coste;
			this.act = act;
			this.padre = padre;
		}

		@Override
		public boolean equals(Object obj) {
			Nodo n = (Nodo) obj;
			boolean prueba = this.x==n.x && this.y==n.y;
			//System.out.println("Prueba comparacion para nodo " + this.x + " " + this.y + " con nodo " + n.x + " " + n.y + ": " + prueba);
			return prueba;
		}
	}
	
	Vector2d fescala, portal, avatar;
	
	ArrayList<ACTIONS> acciones;
	
	
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
		
		acciones = new ArrayList<>();
	}
	
	
	/**
	 * 
	 * @param so
	 * @param et
	 */
	private void caminoDijkastra( StateObservation so, ElapsedCpuTimer et ) {
		PriorityQueue<Nodo> abiertos = new PriorityQueue<>(Comparator.comparingInt(n -> n.coste));
		ArrayList<Nodo> cerrados = new ArrayList<>();
		
		long tInicial = System.nanoTime();
		
		abiertos.add(new Nodo((int)avatar.x, (int)avatar.y, 0));
		
		while (!abiertos.isEmpty()) {
			Nodo n_actual = abiertos.poll();
			
			if (n_actual.x==(int)portal.x && n_actual.y==(int)portal.y) {
				while (n_actual.padre != null) {
					//System.out.println("Nodo: " + n_actual.x + " " + n_actual.y);
					acciones.add(n_actual.act);
					n_actual = n_actual.padre;
				}
				Collections.reverse(acciones);
				break;
			}
			
			cerrados.add(n_actual);
			Nodo n_vecino;
			
			if  (so.getObservationGrid()[n_actual.x][n_actual.y-1].isEmpty() || (n_actual.x==(int)portal.x && n_actual.y-1==(int)portal.y)) {				
				n_vecino = new Nodo(n_actual.x, n_actual.y-1, n_actual.coste+1, Types.ACTIONS.ACTION_UP, n_actual);
				if (!cerrados.contains(n_vecino)) { abiertos.add(n_vecino); }
			}
			
			if  (so.getObservationGrid()[n_actual.x][n_actual.y+1].isEmpty() || (n_actual.x==(int)portal.x && n_actual.y+1==(int)portal.y)) {
				n_vecino = new Nodo(n_actual.x, n_actual.y+1, n_actual.coste+1, Types.ACTIONS.ACTION_DOWN, n_actual);
				if (!cerrados.contains(n_vecino)) { abiertos.add(n_vecino); }
			}
			
			if (so.getObservationGrid()[n_actual.x-1][n_actual.y].isEmpty() || (n_actual.x-1==(int)portal.x && n_actual.y==(int)portal.y)) {
				n_vecino = new Nodo(n_actual.x-1, n_actual.y, n_actual.coste+1, Types.ACTIONS.ACTION_LEFT, n_actual);
				if (!cerrados.contains(n_vecino)) { abiertos.add(n_vecino); }
			}
			
			if (so.getObservationGrid()[n_actual.x+1][n_actual.y].isEmpty() || (n_actual.x+1==(int)portal.x && n_actual.y==(int)portal.y)) {
				n_vecino = new Nodo(n_actual.x+1, n_actual.y, n_actual.coste+1, Types.ACTIONS.ACTION_RIGHT, n_actual);
				if (!cerrados.contains(n_vecino)) { abiertos.add(n_vecino); }
			}
		}
		
		long tFinal = System.nanoTime();
		
		long tTotal = (tFinal-tInicial) / 1000000;
		
		System.out.println("\nTº alg: " + tTotal + "\tTam ruta: " + acciones.size() + "\tNº nodos expand: " + cerrados.size() );
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
		
		if (acciones.isEmpty()) { this.caminoDijkastra(so,et); }
		
		if (!acciones.isEmpty()) {
			return acciones.remove(0);
		}
		else { return Types.ACTIONS.ACTION_NIL; }
	}
	
}
