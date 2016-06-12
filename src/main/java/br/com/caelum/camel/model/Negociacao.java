package br.com.caelum.camel.model;

import java.io.Serializable;
import java.util.Calendar;

public class Negociacao implements Serializable {
    private static final long serialVersionUID = 1L;

    private double preco;
    private int quantidade;
    private Calendar data;

    public double getPreco() {
        return preco;
    }

    public void setPreco(double preco) {
        this.preco = preco;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public Calendar getData() {
        return data;
    }

    public void setData(Calendar data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Negociacao [preco=" + preco + ", quantidade=" + quantidade + "]";
    }
}