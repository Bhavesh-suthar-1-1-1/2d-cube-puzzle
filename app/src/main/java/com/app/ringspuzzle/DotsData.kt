package com.app.ringspuzzle

// Board data measured from the "Rubik's Cube as Permutations" video.
// 54 dots on 9 circles (3 centres x 3 radii). Each circle holds 12 dots;
// a move rotates its 12 dots by 3 slots (quarter turn).
object DotsData {
    // slot positions, normalised: centre (360,770), scale 1/180 of the video frame
    val slots: Array<Pair<Float, Float>> = arrayOf(
        Pair(0.4001f, -0.7943f),
        Pair(-0.4029f, -0.7953f),
        Pair(0.6126f, -0.7441f),
        Pair(-0.6159f, -0.7444f),
        Pair(0.0007f, -0.7338f),
        Pair(0.8119f, -0.6339f),
        Pair(-0.8154f, -0.6333f),
        Pair(-0.1947f, -0.6171f),
        Pair(0.1957f, -0.6164f),
        Pair(-0.4751f, -0.6108f),
        Pair(0.4718f, -0.6102f),
        Pair(0.0004f, -0.5446f),
        Pair(0.6483f, -0.5390f),
        Pair(-0.6517f, -0.5388f),
        Pair(0.3455f, -0.4576f),
        Pair(-0.3451f, -0.4588f),
        Pair(0.4684f, -0.4346f),
        Pair(-0.4718f, -0.4349f),
        Pair(-0.1499f, -0.4280f),
        Pair(0.1502f, -0.4275f),
        Pair(-0.8119f, -0.4050f),
        Pair(0.8086f, -0.4062f),
        Pair(0.6225f, -0.3505f),
        Pair(-0.6258f, -0.3499f),
        Pair(0.0000f, -0.3366f),
        Pair(-0.7494f, -0.1951f),
        Pair(0.7468f, -0.1968f),
        Pair(0.4271f, -0.3160f),
        Pair(0.3035f, -0.1612f),
        Pair(0.4896f, -0.1061f),
        Pair(0.1495f, -0.0762f),
        Pair(0.3294f, 0.0277f),
        Pair(0.1528f, 0.0997f),
        Pair(0.4931f, 0.1222f),
        Pair(0.2936f, 0.2333f),
        Pair(0.0806f, 0.2842f),
        Pair(-0.0012f, 0.3789f),
        Pair(-0.1514f, 0.4698f),
        Pair(0.1487f, 0.4703f),
        Pair(0.3439f, 0.5011f),
        Pair(-0.3467f, 0.4999f),
        Pair(-0.0016f, 0.5869f),
        Pair(-0.1969f, 0.6587f),
        Pair(0.1935f, 0.6594f),
        Pair(-0.0019f, 0.7761f),
        Pair(-0.4291f, -0.3132f),
        Pair(-0.3048f, -0.1595f),
        Pair(-0.4909f, -0.1038f),
        Pair(-0.1507f, -0.0754f),
        Pair(-0.3306f, 0.0290f),
        Pair(-0.1541f, 0.1002f),
        Pair(-0.4942f, 0.1239f),
        Pair(-0.2949f, 0.2341f),
        Pair(-0.0824f, 0.2843f)
    )
    // solved colour at each slot: R Y B G O W
    val solved: String = "BRBRYBRYYRBYBRYYBRYYRBBRYRBOOOOOOOOOWWWWWWWWWGGGGGGGGG"
    // circles: name, centre, radius, 12 slots in angular order, unprimed direction
    val circles: List<Circle> = listOf(
        Circle("U", -0.0017f, -0.5317f, 0.48f, intArrayOf(9, 1, 0, 10, 16, 27, 28, 30, 48, 46, 45, 17), 1),
        Circle("D", -0.0017f, -0.5317f, 0.82f, intArrayOf(6, 5, 21, 26, 33, 34, 35, 53, 52, 51, 25, 20), -1),
        Circle("F", -0.3206f, 0.0206f, 0.48f, intArrayOf(25, 23, 17, 15, 18, 24, 30, 32, 35, 36, 37, 40), 1),
        Circle("B", -0.3206f, 0.0206f, 0.82f, intArrayOf(6, 3, 1, 4, 8, 14, 27, 29, 33, 39, 43, 44), -1),
        Circle("L", 0.3194f, 0.0217f, 0.82f, intArrayOf(47, 45, 15, 7, 4, 0, 2, 5, 44, 42, 40, 51), -1),
        Circle("R", 0.3194f, 0.0217f, 0.48f, intArrayOf(48, 24, 19, 14, 16, 22, 26, 39, 38, 36, 53, 50), 1),
        Circle("M", 0.3194f, 0.0217f, 0.65f, intArrayOf(46, 18, 11, 8, 10, 12, 21, 43, 41, 37, 52, 49), -1),
        Circle("E", -0.0017f, -0.5317f, 0.65f, intArrayOf(13, 3, 2, 12, 22, 29, 31, 32, 50, 49, 47, 23), -1),
        Circle("S", -0.3206f, 0.0206f, 0.65f, intArrayOf(20, 13, 9, 7, 11, 19, 28, 31, 34, 38, 41, 42), 1),
    )
}

data class Circle(val name: String, val cx: Float, val cy: Float, val r: Float, val slots: IntArray, val dir: Int)
