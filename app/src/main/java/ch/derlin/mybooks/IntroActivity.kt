package ch.derlin.mybooks

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import ch.derlin.mybooks.helpers.MiscUtils.attrColor
import ch.derlin.mybooks.helpers.Preferences
import com.github.paolorotolo.appintro.AppIntro
import com.github.paolorotolo.appintro.AppIntroFragment
import com.github.paolorotolo.appintro.model.SliderPage


class IntroActivity : AppIntro() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addSlide(
                getString(R.string.intro_welcome_title),
                getString(R.string.ontro_welcome_msg),
                attrColor(R.attr.colorPrimary),
                R.drawable.splashscreen
        )
        addSlide(
                getString(R.string.intro_integration_title),
                getString(R.string.intro_integration_msg),
                Color.parseColor("#F18805"),
                R.drawable.puzzle
        )
        addSlide(
                getString(R.string.intro_sync_title),
                getString(R.string.intro_sync_msg),
                Color.parseColor("#0093D8"),
                R.drawable.dropbox
        )
        addSlide(
                getString(R.string.intro_theme_title),
                getString(R.string.intro_theme_msg),
                Color.parseColor("#EA4865"),
                R.drawable.paint
        )
        addSlide(
                getString(R.string.intro_last_title),
                getString(R.string.intro_last_msg),
                attrColor(R.attr.colorPrimary),
                R.drawable.splashscreen
        )

        setNavBarColor(R.color.blacky)
        setColorTransitionsEnabled(true)

    }

    private fun addSlide(title: String, description: String, color: Int, drawable: Int, fgColor: Int = -1) {
        val fg = if (fgColor == -1) getColor(R.color.whity) else fgColor

        val sliderPage = SliderPage()
        sliderPage.title = title
        sliderPage.description = description
        sliderPage.imageDrawable = drawable
        sliderPage.bgColor = Color.TRANSPARENT
        sliderPage.titleColor = fg
        sliderPage.descColor = fg
        sliderPage.bgColor = color
        addSlide(AppIntroFragment.newInstance(sliderPage))
    }

    private fun exitIntro() {
        Preferences.introDone = true
        this.finish()
    }

    override fun onSkipPressed(currentFragment: Fragment) {
        super.onSkipPressed(currentFragment)
        exitIntro()
    }

    override fun onDonePressed(currentFragment: Fragment) {
        super.onDonePressed(currentFragment)
        exitIntro()
    }

    companion object {
        const val INTENT_INTRO = 5553
    }

}