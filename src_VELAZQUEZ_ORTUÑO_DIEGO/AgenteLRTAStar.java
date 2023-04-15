package tracks.singlePlayer.evaluacion.src_VELAZQUEZ_ORTUÑO_DIEGO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Pair;
import tools.Vector2d;

public class AgenteLRTAStar extends AbstractPlayer {
	
	Vector2d fescala, portal, avatar;
	
	ArrayList<Pair<ACTIONS,Pair<Integer,Integer>>> act;
	
	HashMap<Integer,Integer> heuristica;
	PriorityQueue<Pair<Integer,ACTIONS>> sucesores;
	
	double tTotal;
	int ruta;
	boolean fin;
	
	/**
	 * in
	 * @param so Observation of the current state.
	 * @param et Timer when the action turned is due.
	 */
	public AgenteLRTAStar( StateObservation so, ElapsedCpuTimer et ) {
		// Calculamos el factor de escala entre mundos (pixeles -> grid)
		fescala = new Vector2d( so.getWorldDimension().width/so.getObservationGrid().length , so.getWorldDimension().height/so.getObservationGrid()[0].length );
		
		// De la lista de portales ordenada por cercanía al avatar, tomamos el más cercano, en nuestro caso solo existirá ese (una sola meta)
		portal = so.getPortalsPositions(so.getAvatarPosition())[0].get(0).position;
		portal.x = Math.floor(portal.x/fescala.x);
		portal.y = Math.floor(portal.y/fescala.y);
		
		act = new ArrayList<Pair<ACTIONS,Pair<Integer,Integer>>>();
		act.add( new Pair<>( ACTIONS.ACTION_UP, new Pair<Integer,Integer>(0,-1) ) );
		act.add( new Pair<>( ACTIONS.ACTION_DOWN, new Pair<Integer,Integer>(0,1) ) );
		act.add( new Pair<>( ACTIONS.ACTION_LEFT, new Pair<Integer,Integer>(-1,0) ) );
		act.add( new Pair<>( ACTIONS.ACTION_RIGHT, new Pair<Integer,Integer>(1,0) ) );
		
		heuristica = new HashMap<>();
		sucesores = new PriorityQueue<>(Comparator.comparingInt(n -> n.first));
		
		tTotal = 0.0;
		ruta = 0;
		fin = false;
	}
	
	
	public ACTIONS act( StateObservation so, ElapsedCpuTimer et ) {
		avatar = new Vector2d( so.getAvatarPosition().x/fescala.x , so.getAvatarPosition().y/fescala.y );
		
		double tInicial = System.nanoTime();
		
		heuristica.putIfAbsent( (int)avatar.y*so.getObservationGrid().length+(int)avatar.x , Math.abs((int)avatar.x-(int)portal.x)+Math.abs((int)avatar.y-(int)portal.y) );
		
		ACTIONS accion = ACTIONS.ACTION_NIL;
		
		for ( Pair<ACTIONS,Pair<Integer,Integer>> a : act ) {
			if (so.getObservationGrid()[(int)avatar.x+a.second.first][(int)avatar.y+a.second.second].isEmpty() || 
					((int)avatar.x+a.second.first==(int)portal.x && (int)avatar.y+a.second.second==(int)portal.y)) {
				
				// en la posición del nodo sucesor añadimos, si no existía antes esta posición, la heurística Manhattan de este nodo
				heuristica.putIfAbsent( ((int)avatar.y+a.second.second)*so.getObservationGrid().length+(int)avatar.x+a.second.first , 
						Math.abs((int)avatar.x+a.second.first-(int)portal.x)+Math.abs((int)avatar.y+a.second.second-(int)portal.y) );
				
				sucesores.add( new Pair<>( heuristica.get( ((int)avatar.y+a.second.second)*so.getObservationGrid().length+(int)avatar.x+a.second.first )+1 , a.first ));
			}
		}
		
		accion = sucesores.peek().second;
		
		heuristica.replace( (int)avatar.y*so.getObservationGrid().length+(int)avatar.x , sucesores.poll().first );
		
		sucesores.clear();
		
		double tFinal = System.nanoTime();
		
		
		for ( Pair<ACTIONS,Pair<Integer,Integer>> a : act ) {
			if (a.first.equals(accion) && ((int)avatar.x+a.second.first==(int)portal.x && (int)avatar.y+a.second.second==(int)portal.y)) { 
				fin = true;
				break;
			}
		}
		
		tTotal += tFinal-tInicial;
		
		ruta++;
		
		if (fin) { System.out.println("\nTº alg: " + tTotal / 1000000 + "\tTam ruta: " + ruta + "\tNº nodos expand: " + ruta); }
		
		return accion;
	}
}





