package com.app.ringspuzzle

// Board data measured from the "Rubik's Cube as Permutations" video.
// 54 dots on 9 circles (3 centres x 3 radii). Each circle holds 12 dots;
// a move rotates its 12 dots by 3 slots (quarter turn).
object DotsData {
    // slot positions, normalised: centre (360,770), scale 1/180 of the video frame
    val slots: Array<Pair<Float, Float>> = arrayOf(
        Pair(0.4582f, -0.7526f),
        Pair(-0.4628f, -0.7519f),
        Pair(0.6231f, -0.7035f),
        Pair(-0.6281f, -0.7035f),
        Pair(-0.0027f, -0.6972f),
        Pair(0.7792f, -0.6157f),
        Pair(-0.7848f, -0.6157f),
        Pair(-0.1562f, -0.6061f),
        Pair(0.1507f, -0.6059f),
        Pair(-0.5076f, -0.5998f),
        Pair(0.5031f, -0.5993f),
        Pair(-0.0028f, -0.5427f),
        Pair(0.6452f, -0.5388f),
        Pair(-0.6516f, -0.5385f),
        Pair(0.2765f, -0.4881f),
        Pair(-0.2821f, -0.4879f),
        Pair(0.5020f, -0.4547f),
        Pair(-0.5068f, -0.4545f),
        Pair(-0.1272f, -0.4508f),
        Pair(0.1220f, -0.4508f),
        Pair(-0.7827f, -0.4359f),
        Pair(0.7778f, -0.4356f),
        Pair(0.6273f, -0.3847f),
        Pair(-0.6319f, -0.3846f),
        Pair(-0.0027f, -0.3776f),
        Pair(-0.7415f, -0.2704f),
        Pair(0.7364f, -0.2698f),
        Pair(0.4166f, -0.2433f),
        Pair(0.3073f, -0.1294f),
        Pair(0.4573f, -0.0766f),
        Pair(0.1813f, -0.0580f),
        Pair(0.3251f, 0.0248f),
        Pair(0.1832f, 0.0866f),
        Pair(0.4588f, 0.1017f),
        Pair(0.3026f, 0.1904f),
        Pair(0.1379f, 0.2386f),
        Pair(-0.0030f, 0.4174f),
        Pair(-0.1271f, 0.4910f),
        Pair(0.1218f, 0.4911f),
        Pair(0.2763f, 0.5277f),
        Pair(-0.2819f, 0.5282f),
        Pair(-0.0028f, 0.5825f),
        Pair(-0.1568f, 0.6462f),
        Pair(0.1513f, 0.6464f),
        Pair(-0.0028f, 0.7370f),
        Pair(-0.4217f, -0.2433f),
        Pair(-0.3128f, -0.1283f),
        Pair(-0.4622f, -0.0778f),
        Pair(-0.1867f, -0.0578f),
        Pair(-0.3306f, 0.0244f),
        Pair(-0.1878f, 0.0861f),
        Pair(-0.4633f, 0.1011f),
        Pair(-0.3083f, 0.1900f),
        Pair(-0.1428f, 0.2383f),
    )
    // solved colour at each slot: R Y B G O W
    val solved: String = "BRBRYBRYYRBYBRYYBRYYRBBRYRBOOOOOOOOOWWWWWWWWWGGGGGGGGG"
    // circles: name, centre, radius, 12 slots in angular order, unprimed direction
    val circles: List<Circle> = listOf(
        Circle("U", -0.0017f, -0.5317f, 0.5094f, intArrayOf(9, 1, 0, 10, 16, 27, 28, 30, 48, 46, 45, 17), 1),
        Circle("D", -0.0017f, -0.5317f, 0.7844f, intArrayOf(6, 5, 21, 26, 33, 34, 35, 53, 52, 51, 25, 20), -1),
        Circle("F", -0.3206f, 0.0206f, 0.5094f, intArrayOf(25, 23, 17, 15, 18, 24, 30, 32, 35, 36, 37, 40), 1),
        Circle("B", -0.3206f, 0.0206f, 0.7844f, intArrayOf(6, 3, 1, 4, 8, 14, 27, 29, 33, 39, 43, 44), -1),
        Circle("L", 0.3194f, 0.0217f, 0.7844f, intArrayOf(47, 45, 15, 7, 4, 0, 2, 5, 44, 42, 40, 51), -1),
        Circle("R", 0.3194f, 0.0217f, 0.5094f, intArrayOf(48, 24, 19, 14, 16, 22, 26, 39, 38, 36, 53, 50), 1),
        Circle("M", 0.3194f, 0.0217f, 0.6472f, intArrayOf(46, 18, 11, 8, 10, 12, 21, 43, 41, 37, 52, 49), -1),
        Circle("E", -0.0017f, -0.5317f, 0.6472f, intArrayOf(13, 3, 2, 12, 22, 29, 31, 32, 50, 49, 47, 23), -1),
        Circle("S", -0.3206f, 0.0206f, 0.6472f, intArrayOf(20, 13, 9, 7, 11, 19, 28, 31, 34, 38, 41, 42), 1),
    )
}

data class Circle(val name: String, val cx: Float, val cy: Float, val r: Float, val slots: IntArray, val dir: Int)
