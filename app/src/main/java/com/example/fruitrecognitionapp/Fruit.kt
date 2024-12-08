package com.example.fruitrecognitionapp

data class Fruit(
    val name: String,
    val nutritionalValues: List<ListItem>,
    val healthBenefits: List<ListItem>)