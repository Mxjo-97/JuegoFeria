/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package juegoferia;

/**
 *
 * @author Isabella Sandoval
 */

import java.util.Map;
public class EstadoJuego {
    private int confianza;
    private int desinformacion;
    private int convivencia;
    private int bienestar;
    private int reputacionJugador;
    private int informacionVerificada;
    private int conflictos;

    public int getConfianza() {
        return confianza;
    }

    public int getDesinformacion() {
        return desinformacion;
    }

    public int getConvivencia() {
        return convivencia;
    }

    public int getBienestar() {
        return bienestar;
    }

    public int getReputacionJugador() {
        return reputacionJugador;
    }

    public int getInformacionVerificada() {
        return informacionVerificada;
    }

    public int getConflictos() {
        return conflictos;
    }
    
    public EstadoJuego() {
        this.confianza=50;
        this.desinformacion=15;
        this.convivencia=50;
        this.bienestar=50;
        this.reputacionJugador=50;
        this.informacionVerificada=0;
        this.conflictos=0;
    }
    
    public void aplicarEfecto (Efecto efecto) {
        if (efecto ==null || efecto.getCambios()==null) {
            return;
        }
        
      Map<String, Integer> Cambios= efecto.getCambios();
      
      for (Map.Entry<String, Integer> Entrada: Cambios.entrySet()) {
          String indicador = Entrada.getKey();
          int valor = Entrada.getValue();
          
          switch(indicador) {
              case "confianza":
                  this.confianza+= valor;
                  break;
              case "convivencia":
                  this.convivencia+= valor;
                  break;
              case "desinformacion":
                  this.desinformacion+= valor;
                  break;
              case "bienestar":
                  this.bienestar+= valor;
                  break;
              case "reputacionjugador":
                  this.reputacionJugador+= valor;
                  break;
              case "informacionverificada":
                  this.informacionVerificada+= valor;
                  break;
              case "conflictos":
                  this.conflictos+= valor;
                  break;
              case "neutro":
                  break;
              
          }
      }
      limitadordeIndicadores();
    }
    
    
    private void limitadordeIndicadores() {
        this.confianza = Math.max(0, Math.min(100, this.confianza));
        this.desinformacion = Math.max(0, Math.min(100, this.desinformacion));
        this.convivencia = Math.max(0, Math.min(100, this.convivencia));
        this.bienestar = Math.max(0, Math.min(100, this.bienestar));
        this.reputacionJugador = Math.max(0, Math.min(100, this.reputacionJugador));
        this.conflictos = Math.max(0, Math.min(100, this.conflictos));
    }
    
    public void imprimirEstadoPartida() {
        System.out.println("--- Numeros de las variables en la Partida ---");
        System.out.println("Confianza: " + confianza);
        System.out.println("Desinformacion: " + desinformacion);
        System.out.println("Convivencia: " + convivencia);
        System.out.println("Bienestar: " + bienestar);
        System.out.println("Reputacion Jugador: " + reputacionJugador);
        System.out.println("Informacion Verificada: " + informacionVerificada);
        System.out.println("Conflictos: " + conflictos);
        System.out.println("----------------------------------");
}
}
