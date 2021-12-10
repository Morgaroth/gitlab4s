import sbt.{KeyRanks, SettingKey}

object Syntax {

  implicit class RichSettingKey[T](k: SettingKey[T]) {
    def invisible: SettingKey[T] = k.withRank(KeyRanks.Invisible)
  }

}
