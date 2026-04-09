package io.github.mmarco94.tambourine.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import kotlin.io.path.Path

class FileTest : FunSpec({
    context("pathSimilarity") {
        withData(
            tuple("a/b/c.mp3", "a.mp3", 0),
            tuple("a/b/c.mp3", "c.mp3", 1),
            tuple("a/b/c.mp3", "b/c.mp3", 2),
            tuple("a/b/c.mp3", "a/b/c.mp3", 3),
            tuple("a/b/c.mp3", "/a/b/c.mp3", 3),
            tuple("a/b/c.mp3", "d/a/b/c.mp3", 3),
            tuple("/a/b/c.mp3", "d/a/b/c.mp3", 3),
            tuple("/a/b/c.mp3", "/a/b/c.mp3", 4),
        ) { (p1, p2, expected) ->
            pathSimilarity(Path(p1), Path(p2)) shouldBe expected
        }
    }
})
