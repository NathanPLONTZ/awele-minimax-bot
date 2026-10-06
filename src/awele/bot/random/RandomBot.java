package awele.bot.random;

import java.util.Random;

import awele.bot.DemoBot;
import awele.core.Board;
import awele.core.InvalidBotException;

/**
 * @author Nathan PLONTZ
 * Example stub: a bot that plays at random.
 * Superseded by awele.bot.demo.random.RandomBot; body left commented out.
 */
public class RandomBot {
	/*
	 * extends DemoBot { private Random random;
	 * 
	 *//**
		 * @throws InvalidBotException
		 */
	/*
	 * public RandomBot () throws InvalidBotException { this.setBotName ("Random");
	 * this.addAuthor ("Nathan PLONTZ"); }
	 * 
	 *//**
		 * Rien à faire
		 */
	/*
	 * @Override public void initialize () { }
	 * 
	 *//**
		 * La priorité de chaque coup est donné au hasard
		 */
	/*
	 * @Override public double [] getDecision (Board board) { double [] decision =
	 * new double [Board.NB_HOLES]; for (int i = 0; i < decision.length; i++)
	 * decision [i] = this.random.nextDouble (); return decision; }
	 * 
	 *//**
		 * Initialisation de la génération pseudo-aléatoire de nombre
		 */
	/*
	 * @Override public void learn () { this.random = new Random
	 * (System.currentTimeMillis ()); }
	 * 
	 *//**
		 * Rien à faire
		 *//*
			 * @Override public void finish () { }
			 */
}
