package com.ecoverde.alimentar

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ecoverde.alimentar.db.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    private lateinit var foodNameInput: android.widget.EditText
    private lateinit var foodQuantityInput: android.widget.EditText
    private lateinit var searchFoodInput: android.widget.EditText
    private lateinit var categoryFilterSpinner: android.widget.Spinner
    private lateinit var saveFoodButton: android.widget.Button
    private lateinit var clearFoodButton: android.widget.Button
    private lateinit var suggestionButton: android.widget.Button
    private lateinit var summaryTextView: android.widget.TextView

    private lateinit var recipeNameInput: android.widget.EditText
    private lateinit var recipeIngredientsInput: android.widget.EditText
    private lateinit var recipeDescriptionInput: android.widget.EditText
    private lateinit var recipeCategorySpinner: android.widget.Spinner
    private lateinit var saveRecipeButton: android.widget.Button
    private lateinit var clearRecipeButton: android.widget.Button

    private lateinit var foodRecyclerView: RecyclerView
    private lateinit var recipeRecyclerView: RecyclerView

    private val categoryOptions = listOf("Todas", "frutas", "verduras", "legumes", "graos", "outros")
    private val recipeCategoryOptions = listOf("Receita", "Sobremesa", "Sopa", "Salada", "Prato principal", "Bolo", "Suco")

    private var selectedFoodId: Long? = null
    private var selectedRecipeId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DatabaseHelper(this)

        foodNameInput = findViewById(R.id.foodNameInput)
        foodQuantityInput = findViewById(R.id.foodQuantityInput)
        searchFoodInput = findViewById(R.id.searchFoodInput)
        categoryFilterSpinner = findViewById(R.id.categoryFilterSpinner)
        saveFoodButton = findViewById(R.id.saveFoodButton)
        clearFoodButton = findViewById(R.id.clearFoodButton)
        suggestionButton = findViewById(R.id.suggestionButton)
        summaryTextView = findViewById(R.id.summaryTextView)

        recipeNameInput = findViewById(R.id.recipeNameInput)
        recipeIngredientsInput = findViewById(R.id.recipeIngredientsInput)
        recipeDescriptionInput = findViewById(R.id.recipeDescriptionInput)
        recipeCategorySpinner = findViewById(R.id.recipeCategorySpinner)
        saveRecipeButton = findViewById(R.id.saveRecipeButton)
        clearRecipeButton = findViewById(R.id.clearRecipeButton)

        foodRecyclerView = findViewById(R.id.foodRecyclerView)
        recipeRecyclerView = findViewById(R.id.recipeRecyclerView)

        setupSpinners()
        setupFoodRecycler()
        setupRecipeRecycler()
        setupListeners()
        refreshFoods()
        refreshRecipes()
    }

    private fun setupSpinners() {
        val foodAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryOptions)
        foodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoryFilterSpinner.adapter = foodAdapter

        val recipeAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, recipeCategoryOptions)
        recipeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        recipeCategorySpinner.adapter = recipeAdapter
        recipeCategorySpinner.setSelection(0)
    }

    private fun setupListeners() {
        saveFoodButton.setOnClickListener {
            val name = foodNameInput.text.toString().trim()
            val quantity = foodQuantityInput.text.toString().trim()

            if (name.isEmpty() || quantity.isEmpty()) {
                Toast.makeText(this, "Preencha nome e quantidade", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = recognizeFood(name)
            val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            if (selectedFoodId != null) {
                db.updateFood(selectedFoodId!!, name, quantity, category, date)
                Toast.makeText(this, "Alimento atualizado", Toast.LENGTH_SHORT).show()
            } else {
                db.insertFood(name, quantity, category, date)
                Toast.makeText(this, "Alimento cadastrado", Toast.LENGTH_SHORT).show()
            }

            clearFoodForm()
            refreshFoods()
        }

        clearFoodButton.setOnClickListener { clearFoodForm() }

        suggestionButton.setOnClickListener {
            val name = foodNameInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Digite um alimento para ver a sugestão", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val suggestion = suggestFood(name)
            AlertDialog.Builder(this)
                .setTitle("Sugestão de reaproveitamento")
                .setMessage(suggestion)
                .setPositiveButton("OK", null)
                .show()
        }

        searchFoodInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { refreshFoods() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        categoryFilterSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                refreshFoods()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        saveRecipeButton.setOnClickListener {
            val nome = recipeNameInput.text.toString().trim()
            val ingredientes = recipeIngredientsInput.text.toString().trim()
            val descricao = recipeDescriptionInput.text.toString().trim()
            val categoria = recipeCategorySpinner.selectedItem.toString()

            if (nome.isEmpty() || ingredientes.isEmpty() || descricao.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos da receita", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedRecipeId != null) {
                db.updateRecipe(selectedRecipeId!!, nome, ingredientes, descricao, categoria)
                Toast.makeText(this, "Receita atualizada", Toast.LENGTH_SHORT).show()
            } else {
                db.insertRecipe(nome, ingredientes, descricao, categoria)
                Toast.makeText(this, "Receita cadastrada", Toast.LENGTH_SHORT).show()
            }

            clearRecipeForm()
            refreshRecipes()
        }

        clearRecipeButton.setOnClickListener { clearRecipeForm() }
    }

    private fun setupFoodRecycler() {
        val adapter = FoodAdapter(
            onClick = { food ->
                selectedFoodId = food.id
                foodNameInput.setText(food.name)
                foodQuantityInput.setText(food.quantity)
                saveFoodButton.text = "Atualizar"
            },
            onDelete = { food ->
                AlertDialog.Builder(this)
                    .setTitle("Excluir alimento")
                    .setMessage("Deseja excluir ${food.name}?")
                    .setPositiveButton("Sim") { _, _ ->
                        db.deleteFood(food.id)
                        refreshFoods()
                        if (selectedFoodId == food.id) clearFoodForm()
                    }
                    .setNegativeButton("Não", null)
                    .show()
            }
        )

        foodRecyclerView.layoutManager = LinearLayoutManager(this)
        foodRecyclerView.adapter = adapter
    }

    private fun setupRecipeRecycler() {
        val adapter = RecipeAdapter(
            onClick = { recipe ->
                selectedRecipeId = recipe.id
                recipeNameInput.setText(recipe.name)
                recipeIngredientsInput.setText(recipe.ingredients)
                recipeDescriptionInput.setText(recipe.description)
                recipeCategorySpinner.setSelection(recipeCategoryOptions.indexOf(recipe.category).takeIf { it >= 0 } ?: 0)
                saveRecipeButton.text = "Atualizar receita"
            },
            onDelete = { recipe ->
                AlertDialog.Builder(this)
                    .setTitle("Excluir receita")
                    .setMessage("Deseja excluir ${recipe.name}?")
                    .setPositiveButton("Sim") { _, _ ->
                        db.deleteRecipe(recipe.id)
                        refreshRecipes()
                        if (selectedRecipeId == recipe.id) clearRecipeForm()
                    }
                    .setNegativeButton("Não", null)
                    .show()
            }
        )

        recipeRecyclerView.layoutManager = LinearLayoutManager(this)
        recipeRecyclerView.adapter = adapter
    }

    private fun refreshFoods() {
        val adapter = foodRecyclerView.adapter as FoodAdapter
        val search = searchFoodInput.text.toString().trim()
        val filter = categoryFilterSpinner.selectedItem?.toString() ?: "Todas"
        val foods = db.getFoods(search, filter)
        adapter.submitList(foods)

        val summary = db.getSummary()
        val builder = StringBuilder()
        builder.append("Total de alimentos: ${db.countFoods()}\n")
        if (summary.isNotEmpty()) {
            for ((category, total) in summary) {
                builder.append("• ${category.capitalize()} : $total\n")
            }
        }
        summaryTextView.text = builder.toString().trim()
    }

    private fun refreshRecipes() {
        val adapter = recipeRecyclerView.adapter as RecipeAdapter
        val search = searchFoodInput.text.toString().trim()
        adapter.submitList(db.getRecipes(search))
    }

    private fun clearFoodForm() {
        selectedFoodId = null
        foodNameInput.setText("")
        foodQuantityInput.setText("")
        saveFoodButton.text = "Salvar"
    }

    private fun clearRecipeForm() {
        selectedRecipeId = null
        recipeNameInput.setText("")
        recipeIngredientsInput.setText("")
        recipeDescriptionInput.setText("")
        recipeCategorySpinner.setSelection(0)
        saveRecipeButton.text = "Salvar receita"
    }

    private fun recognizeFood(name: String): String {
        val normalized = normalize(name)
        for ((category, items) in categoryMap()) {
            for (item in items) {
                if (normalize(item) == normalized) return category
            }
        }
        return "não identificado"
    }

    private fun suggestFood(name: String): String {
        val normalized = normalize(name)
        val suggestions = mapOf(
            "banana" to "Use em vitaminas, bolos ou panquecas.",
            "maca" to "Aproveite em bolos, compotas ou sucos.",
            "laranja" to "Faça sucos, sobremesas ou chá de casca.",
            "alface" to "Use em saladas, sucos verdes ou sopas.",
            "cenoura" to "Aproveite em sopas, purês ou saladas.",
            "batata" to "Use em purês, sopas ou bolinhos.",
            "tomate" to "Ótimo para molhos, saladas e pratos quentes.",
            "arroz" to "Transforme em bolinhos, arroz doce ou sopa.",
            "feijao" to "Pode ser usado em sopas, refogados e croquetes.",
            "macarrao" to "Aproveite em frittatas, bolinhos ou sopas.",
            "ovo" to "Use em omeletes, bolos e saladas."
        )

        return suggestions[normalized] ?: "Procure uma receita adequada para esse alimento e reduza o desperdício!"
    }

    private fun normalize(text: String): String {
        if (text.isBlank()) return ""
        val normalized = text.lowercase().trim()
            .replace("ã", "a").replace("á", "a").replace("à", "a").replace("â", "a")
            .replace("ẽ", "e").replace("é", "e").replace("è", "e").replace("ê", "e")
            .replace("ĩ", "i").replace("í", "i").replace("ì", "i").replace("î", "i")
            .replace("õ", "o").replace("ó", "o").replace("ò", "o").replace("ô", "o")
            .replace("ũ", "u").replace("ú", "u").replace("ù", "u").replace("û", "u")
            .replace("ç", "c")
        return normalized.replace(" ", "")
    }

    private fun categoryMap(): Map<String, List<String>> {
        return mapOf(
            "frutas" to listOf("banana", "maçã", "maca", "laranja", "mamão", "mamao", "uva"),
            "verduras" to listOf("alface", "couve", "espinafre", "brócolis", "brocolis", "repolho"),
            "legumes" to listOf("cenoura", "batata", "tomate", "abobrinha", "pepino", "beterraba"),
            "graos" to listOf("arroz", "feijão", "feijao", "macarrão", "macarrao", "lentilha"),
            "outros" to listOf("leite", "iogurte", "ovo", "pão", "pao", "queijo")
        )
    }
}

class FoodAdapter(
    private val onClick: (Food) -> Unit,
    private val onDelete: (Food) -> Unit
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    private val items = mutableListOf<Food>()

    fun submitList(newItems: List<Food>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, onClick, onDelete)
    }

    override fun getItemCount(): Int = items.size

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.itemNameText)
        private val quantityText: TextView = itemView.findViewById(R.id.itemQuantityText)
        private val categoryText: TextView = itemView.findViewById(R.id.itemCategoryText)

        fun bind(item: Food, onClick: (Food) -> Unit, onDelete: (Food) -> Unit) {
            nameText.text = item.name
            quantityText.text = item.quantity
            categoryText.text = item.category

            itemView.setOnClickListener { onClick(item) }
            itemView.setOnLongClickListener {
                onDelete(item)
                true
            }
        }
    }
}

class RecipeAdapter(
    private val onClick: (Recipe) -> Unit,
    private val onDelete: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    private val items = mutableListOf<Recipe>()

    fun submitList(newItems: List<Recipe>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, onClick, onDelete)
    }

    override fun getItemCount(): Int = items.size

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.recipeNameText)
        private val categoryText: TextView = itemView.findViewById(R.id.recipeCategoryText)

        fun bind(item: Recipe, onClick: (Recipe) -> Unit, onDelete: (Recipe) -> Unit) {
            nameText.text = item.name
            categoryText.text = item.category

            itemView.setOnClickListener { onClick(item) }
            itemView.setOnLongClickListener {
                onDelete(item)
                true
            }
        }
    }
}
