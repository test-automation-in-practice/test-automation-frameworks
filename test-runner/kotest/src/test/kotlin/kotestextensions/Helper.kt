package kotestextensions

import io.kotest.core.spec.BeforeEach
import io.mockk.clearAllMocks

val clearAllMocks: BeforeEach = { clearAllMocks() }
