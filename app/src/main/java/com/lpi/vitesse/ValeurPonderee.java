package com.lpi.vitesse;

import java.util.Calendar;
import java.util.LinkedList;

public class ValeurPonderee
{

	private static class ValeurRecue
	{
		public float _valeur;
		public Calendar _date;

		public ValeurRecue(float valeur)
		{
			_valeur = valeur;
			_date = Calendar.getInstance();
		}
	}

	private final LinkedList<ValeurRecue> _dernieresValeurs = new LinkedList<>();
	private float _valeurLissée =0;
	private int _delaiLissageSecondes;

	public ValeurPonderee(int delaiLissageSecondes)
	{
		_delaiLissageSecondes = delaiLissageSecondes;
	}

	/***
	 * Ajoute une valeur et calcule la nouvelle valeur lissée
	 * @param valeur
	 */
	public void ajouteValeur(float valeur)
	{
		// Nouveau cap lissé
		_dernieresValeurs.add(new ValeurRecue(valeur));

		// Eliminer les valeurs trop anciennes
		Calendar debut = Calendar.getInstance();
		debut.roll(Calendar.SECOND, -_delaiLissageSecondes);
		while ((_dernieresValeurs.size() > 0) && (_dernieresValeurs.getFirst()._date.before(debut)))
			_dernieresValeurs.removeFirst();

		// Valeur moyenne du cap
		_valeurLissée = 0;
		for (ValeurRecue f : _dernieresValeurs)
			_valeurLissée += f._valeur;
		_valeurLissée /= _dernieresValeurs.size(); // Size superieur à 0, puisqu'on vient d'ajouter une valeur
	}

	public boolean hasValue(){ return  !_dernieresValeurs.isEmpty();}
	public float getValue() { return _valeurLissée;}
	public void setDelai(int valeur)
	{
		_delaiLissageSecondes = valeur;
	}

}
