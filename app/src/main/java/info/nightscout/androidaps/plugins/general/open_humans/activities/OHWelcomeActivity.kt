package info.nightscout.androidaps.plugins.general.open_humans.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import info.nightscout.androidaps.databinding.Openhumans1Binding

class OHWelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = Openhumans1Binding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.back.setOnClickListener { finish() }
        binding.next.setOnClickListener { startActivityForResult(Intent(this@OHWelcomeActivity, OHLoginActivity::class.java), REQUEST_CODE) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) finish()
    }

    companion object {
        const val REQUEST_CODE = 515;
    }
}