package awele.bot.demo.knn1;

import awele.bot.DemoBot;
import awele.core.Board;
import awele.core.InvalidBotException;

/**
 * @author Nathan PLONTZ
 * Example stub: first k-NN bot, rating a move by how many of the position's
 * nearest neighbours in the dataset played it. Body left commented out;
 * the lookup itself now lives in awele.bot.common.KnnData.
 */
public class Knn1Bot {
	/*
	 * extends DemoBot { private static final int k = 10; private Knn1Data data;
	 * 
	 *//**
		 * @throws InvalidBotException
		 */
	/*
	 * public Knn1Bot () throws InvalidBotException { this.setBotName ("k-NN1");
	 * this.addAuthor ("Nathan PLONTZ"); }
	 * 
	 *//**
		 * Rien à faire
		 */
	/*
	 * @Override public void initialize () { }
	 * 
	 *//**
		 * Pour une situation donnée, on regarde les situations les plus proches dans la
		 * base de données La priorité pour chaque coup est donnée par le nombre de
		 * voisins pour ce coup
		 */
	/*
	 * @Override public double [] getDecision (Board board) { int [] x = new int
	 * [12]; int [] holes = board.getPlayerHoles (); for (int i = 0; i < 6; i++) x
	 * [i] = holes [i]; holes = board.getOpponentHoles (); for (int i = 0; i < 6;
	 * i++) x [i + 6] = holes [i]; return this.data.countNeighbors (x, Knn1Bot.k); }
	 * 
	 *//**
		 * Création des données
		 */
	/*
	 * @Override public void learn () { this.data = new Knn1Data (); }
	 * 
	 *//**
		 * Rien à faire
		 *//*
			 * @Override public void finish () { }
			 */
}
