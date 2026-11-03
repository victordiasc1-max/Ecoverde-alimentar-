package com.ecoverde.alimentar

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.Context

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "eco_verde_alimentar.db"
        const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS alimentos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nome TEXT NOT NULL, " +
                "quantidade TEXT NOT NULL, " +
                "categoria TEXT NOT NULL, " +
                "data TEXT NOT NULL)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS receitas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nome TEXT NOT NULL, " +
                "ingredientes TEXT NOT NULL, " +
                "descricao TEXT NOT NULL, " +
                "categoria TEXT NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS alimentos")
        db.execSQL("DROP TABLE IF EXISTS receitas")
        onCreate(db)
    }

    fun insertFood(name: String, quantity: String, category: String, date: String): Long {
        val db = writableDatabase
        val values = android.content.ContentValues().apply {
            put("nome", name)
            put("quantidade", quantity)
            put("categoria", category)
            put("data", date)
        }
        return db.insert("alimentos", null, values)
    }

    fun updateFood(id: Long, name: String, quantity: String, category: String, date: String): Int {
        val db = writableDatabase
        val values = android.content.ContentValues().apply {
            put("nome", name)
            put("quantidade", quantity)
            put("categoria", category)
            put("data", date)
        }
        return db.update("alimentos", values, "id = ?", arrayOf(id.toString()))
    }

    fun deleteFood(id: Long): Int {
        val db = writableDatabase
        return db.delete("alimentos", "id = ?", arrayOf(id.toString()))
    }

    fun getFoods(search: String = "", category: String = "Todas"): List<Food> {
        val db = readableDatabase
        val queryBuilder = StringBuilder("SELECT * FROM alimentos WHERE 1 = 1")
        val args = mutableListOf<String>()

        if (search.isNotBlank()) {
            queryBuilder.append(" AND (lower(nome) LIKE ? OR lower(quantidade) LIKE ? OR lower(categoria) LIKE ?)")
            val likeSearch = "%${search.lowercase()}%"
            args.add(likeSearch)
            args.add(likeSearch)
            args.add(likeSearch)
        }

        if (category != "Todas") {
            queryBuilder.append(" AND categoria = ?")
            args.add(category)
        }

        queryBuilder.append(" ORDER BY data DESC, nome ASC")

        val cursor = db.rawQuery(queryBuilder.toString(), args.toTypedArray())
        val foods = mutableListOf<Food>()
        while (cursor.moveToNext()) {
            foods.add(
                Food(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("nome")),
                    quantity = cursor.getString(cursor.getColumnIndexOrThrow("quantidade")),
                    category = cursor.getString(cursor.getColumnIndexOrThrow("categoria")),
                    date = cursor.getString(cursor.getColumnIndexOrThrow("data"))
                )
            )
        }
        cursor.close()
        return foods
    }

    fun getFoodById(id: Long): Food? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM alimentos WHERE id = ?", arrayOf(id.toString()))
        var food: Food? = null
        if (cursor.moveToFirst()) {
            food = Food(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("nome")),
                quantity = cursor.getString(cursor.getColumnIndexOrThrow("quantidade")),
                category = cursor.getString(cursor.getColumnIndexOrThrow("categoria")),
                date = cursor.getString(cursor.getColumnIndexOrThrow("data"))
            )
        }
        cursor.close()
        return food
    }

    fun getSummary(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT categoria, COUNT(*) AS total FROM alimentos GROUP BY categoria ORDER BY total DESC",
            null
        )

        while (cursor.moveToNext()) {
            val category = cursor.getString(cursor.getColumnIndexOrThrow("categoria"))
            val total = cursor.getInt(cursor.getColumnIndexOrThrow("total"))
            map[category] = total
        }
        cursor.close()
        return map
    }

    fun countFoods(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM alimentos", null)
        var count = 0
        if (cursor.moveToFirst()) count = cursor.getInt(0)
        cursor.close()
        return count
    }

    fun insertRecipe(name: String, ingredients: String, description: String, category: String): Long {
        val db = writableDatabase
        val values = android.content.ContentValues().apply {
            put("nome", name)
            put("ingredientes", ingredients)
            put("descricao", description)
            put("categoria", category)
        }
        return db.insert("receitas", null, values)
    }

    fun updateRecipe(id: Long, name: String, ingredients: String, description: String, category: String): Int {
        val db = writableDatabase
        val values = android.content.ContentValues().apply {
            put("nome", name)
            put("ingredientes", ingredients)
            put("descricao", description)
            put("categoria", category)
        }
        return db.update("receitas", values, "id = ?", arrayOf(id.toString()))
    }

    fun deleteRecipe(id: Long): Int {
        val db = writableDatabase
        return db.delete("receitas", "id = ?", arrayOf(id.toString()))
    }

    fun getRecipes(search: String = ""): List<Recipe> {
        val db = readableDatabase
        val query = if (search.isBlank()) {
            "SELECT * FROM receitas ORDER BY nome ASC"
        } else {
            "SELECT * FROM receitas WHERE lower(nome) LIKE ? OR lower(ingredientes) LIKE ? OR lower(descricao) LIKE ? OR lower(categoria) LIKE ? ORDER BY nome ASC"
        }

        val args = if (search.isBlank()) {
            emptyArray<String>()
        } else {
            val s = "%${search.lowercase()}%"
            arrayOf(s, s, s, s)
        }

        val cursor = db.rawQuery(query, args)
        val list = mutableListOf<Recipe>()
        while (cursor.moveToNext()) {
            list.add(
                Recipe(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("nome")),
                    ingredients = cursor.getString(cursor.getColumnIndexOrThrow("ingredientes")),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("descricao")),
                    category = cursor.getString(cursor.getColumnIndexOrThrow("categoria"))
                )
            )
        }
        cursor.close()
        return list
    }

    fun countRecipes(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM receitas", null)
        var count = 0
        if (cursor.moveToFirst()) count = cursor.getInt(0)
        cursor.close()
        return count
    }
}
