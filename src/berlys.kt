import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Costumer (val id: Int, val name: String, val town: String, val PVL: Float)

data class Route (val id: Int, val name: String, val date: String, val costumers: MutableList<Costumer>)

class SourceFile {
    private val formatter = Formatter()
    private val sep: String = File.separator
    private var sourceFilename: String = "Volumen Rutas.txt"
    private var readDir: String = listOf("C:", "Users", "igorr", "Downloads").joinToString(sep)
    private val writeDirArray = listOf(
        "C:", "Users", "igorr", "OneDrive", "Eclipse", "Python", "dades", "Berlys"
    )
    private var writeDir: String = writeDirArray.joinToString (sep)
    private var date = LocalDateTime.now()
    private var file: File = File("")

    fun checkDate(): LocalDateTime {
        if (date.hour > 18 && LocalDateTime.now().dayOfMonth == date.dayOfMonth)
            date = date.plusDays(1)
        return date
    }

    fun checkWriteDir() {
        // Create new folder when it doesn't exist.

        writeDir = listOf(writeDir, date.year, "%02d".format(date.monthValue)).joinToString(sep)
        if (!File(writeDir).exists()) {
            File(writeDir).mkdir()
        }
    }

    fun checkFile() {
        /*
            Read downloaded file from Download folder, if it isn't there then read it from data folder.
            If file is in Download folder, move it to data folder.
         */
        file = File(readDir, sourceFilename)
        checkDate()
        checkWriteDir()

        if (!file.exists()) {
            val files = File(writeDir).listFiles()
            val filename = files[files.lastIndex]
            file = File(filename.toURI())
        } else {
            val filename = listOf(
                writeDir, date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            ).joinToString(sep) + ".txt"
            file.renameTo(File(filename))
            file = File(filename)
        }
    }

    private fun readSourceFile() : String {
        checkFile()
        var data: String = file.readText()
        return data
    }

    fun fetchData(allowedRoutes: List<Int>) : MutableMap<Int, Route> {
        val routes : MutableMap<Int, Route> = mutableMapOf()
        val routePattern = Regex(
            "25\\s+BERLYS ALIMENTACION S\\.A\\.U\\s+[\\d:]+\\s+[\\d.]+\\s+Volumen de pedidos de la ruta :\\s+" +
                "(?<routeid>\\d+)\\s+(?<routedesc>[^\\n]+)\\s+Día de entrega :\\s+(?<date>[^ ]{10})(?<stops>.+?)" +
                "NUMERO DE CLIENTES\\s+:\\s+(?<costnum>\\d+).+?" +
                "SUMA VOLUMEN POR RUTA\\s+:\\s+(?<volamt>[\\d,.]+) (?<um1>(?:PVL|KG)).+?" +
                "SUMA KG POR RUTA\\s+:\\s+(?<weightamt>[\\d,.]+) (?<um2>(?:PVL|KG)).+?" +
                "(?:CAPACIDAD TOTAL CAMIÓN\\s+:\\s+(?<truckcap>[\\d,.]+) (?<um3>(?:PVL|KG)))?", RegexOption.DOT_MATCHES_ALL
            )
        val costumersPattern = Regex(
            "(?<code>\\d{10}) (?<costumer>.{35}) (?<town>.{20}) (?<ordnum>.{10}) (?<vol>.{11})(?: (?<UM>.{2,3}))?"
        )
        val text = readSourceFile()

        //Routes iteration
        routePattern.findAll(text).forEach { matchResult ->
            val routeid: Int = matchResult.groups["routeid"]!!.value.toInt()
            if (routeid in allowedRoutes) {
                val items = costumersPattern.findAll(matchResult.groups["stops"]!!.value)
                var costumers = mutableListOf<Costumer>()

                //Costumers iteration
                items.forEach {
                    val costumer = Costumer(
                        it.groups["code"]!!.value.toInt(),
                        it.groups["costumer"]!!.value.trim(' '),
                        it.groups["town"]!!.value.trim(' '),
                        formatter.strToFloat(it.groups["vol"]!!.value)
                    )
                    costumers.add(costumer)
                }
                val route = Route(
                    matchResult.groups["routeid"]!!.value.toInt(),
                    matchResult.groups["routedesc"]!!.value.trimStart('2', '5', ' '),
                    matchResult.groups["date"]!!.value,
                    costumers
                )
                routes[route.id] = route
            }
        }
        return routes
    }
}

class Berlys {
    private val source = SourceFile()
    private var date = source.checkDate()
    private val knownRoutes : List<Int> = listOf(678, 679, 680, 681, 682, 686, 688, 696)
    private var routes : MutableMap<Int, Route> = mutableMapOf()
    private val formatter: Formatter = Formatter()

    fun showRoutes () {
        var i = 1
        for ((routeid, route) in routes) {
            println(route.date + " - " + routeid.toString() + " - " + route.name)
            route.costumers.forEach { costumer ->
                println("${i++}\t${costumer.name}\t${costumer.town}\t${costumer.PVL} PVL")
            }
            println()
        }
    }

    fun showAssignedRoutes(assignedRoutes: List<Int>) {
        var i = 0
        assignedRoutes.forEach { assignedRoute ->
            val route = routes[assignedRoute]
            if (route != null) {
                println("${route.date}\t${route.id}\t${route.name}")
                route.costumers.forEach { costumer ->
                    println("${++i}\t\t${costumer.name}\t${formatter.floatToStr(costumer.PVL)}\t${costumer.town}")
                }
                println()
            }
        }
    }

    fun loadData() {
        routes = source.fetchData(knownRoutes)
    }

    fun run (){
        val weekDays = mapOf<Int, List<Int>>(
            1 to listOf(680, 681),
            2 to listOf(680, 681),
            3 to listOf(680, 681),
            4 to listOf(680, 681),
            5 to listOf(680, 681),
            6 to listOf(680, 681)
        )
        println(date.toString())
        val assignedRoutes = weekDays[date.dayOfWeek.value]
        if (weekDays.containsKey(date.dayOfWeek.value))
            showAssignedRoutes(assignedRoutes!!)
        else
            throw Exception("Today you must to take a pause.")
     }
}

fun convertToIntList(params: Array<String>) : List<Int>{
    val list = mutableListOf<Int>()
    params.forEach {
        list.add(it.toInt())
    }
    return list
}

class Formatter {
    val number: NumberFormat = NumberFormat.getInstance()
    val decimalFormat = DecimalFormat.getInstance()

    fun strToFloat(string: String): Float {
        // Convert String to Float.
        return number.parse(string.trim()).toFloat()
    }


    fun floatToStr(float: Float): String {
        // Convert Float to String.
        return decimalFormat.format(float)
    }
}


fun main(args: Array<String>) {
    val formatter = Formatter()

    val berlys = Berlys()
    berlys.loadData()

    if (args.isNotEmpty()) {
        if (args[0] == "--all") {
            berlys.showRoutes()
        } else if (args.size > 1) {
            berlys.showAssignedRoutes(convertToIntList(args))
        }
    } else
        berlys.run()
    println(formatter.floatToStr(1901924.12f))
}