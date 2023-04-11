package org.cosmicide.rewrite.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.build.BuildReporter
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.compile.Compiler
import org.cosmicide.rewrite.databinding.FragmentCompileInfoBinding
import org.cosmicide.rewrite.extension.setFont
import org.cosmicide.rewrite.util.ProjectHandler

/**
 * A fragment for displaying information about the compilation process.
 */
class CompileInfoFragment : BaseBindingFragment<FragmentCompileInfoBinding>() {
    val project: Project = ProjectHandler.getProject()
        ?: throw IllegalStateException("No project set")
    val reporter by lazy {
        BuildReporter { report ->
            if (report.message.isEmpty()) return@BuildReporter
            // Update the info editor with the build output
            val text = binding.infoEditor.text
            val cursor = text.cursor
            text.insert(
                cursor.rightLine,
                cursor.rightColumn,
                "${report.kind}: ${report.message}\n"
            )
        }
    }
    val compiler: Compiler = Compiler(project, reporter)

    override fun getViewBinding() = FragmentCompileInfoBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.infoEditor.apply {
            setEditorLanguage(TextMateLanguage.create("source.build", false))
            colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            editable = false
            setTextSize(16f)
            isWordwrap = true
            isLineNumberEnabled = false
            setFont()
        }

        binding.toolbar.title = "Compiling ${project.name}"
        binding.toolbar.setNavigationOnClickListener {
            navigateToProjectOutputFragment()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                compiler.compile()
                if (reporter.buildSuccess) {
                    withContext(Dispatchers.Main) {
                        navigateToProjectOutputFragment()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.infoEditor.setText(e.stackTraceToString())
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.infoEditor.release()
        super.onDestroyView()
    }

    @SuppressLint("CommitTransaction")
    private fun navigateToProjectOutputFragment() {
        parentFragmentManager.beginTransaction()
            .add(R.id.fragment_container, ProjectOutputFragment())
            .addToBackStack(null)
            .commit()
    }
}