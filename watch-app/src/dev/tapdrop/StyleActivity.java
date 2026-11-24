package dev.tapdrop;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/**
 * Style: the app's look, One UI or Material 3 Expressive, or Automatic, which
 * follows the watch's maker. The screen redraws in the new look as soon as one is
 * chosen, so the wearer sees it before going back.
 */
public class StyleActivity extends Activity {
    private LinearLayout column;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        build();
    }

    private void build() {
        String choice = Theme.choice(this);
        column = Ui.column(this);
        Theme theme = Ui.theme(this);
        column.addView(Ui.title(this, "Style"));

        LinearLayout choices = Ui.group(this);
        option(choices, Theme.AUTO, "Automatic",
            (Theme.isSamsung() ? "Galaxy Watch: " : "This watch: ") + Theme.automaticName(), choice);
        option(choices, Theme.ONE_UI, Theme.ONE_UI_NAME, "Samsung's watch look", choice);
        option(choices, Theme.MATERIAL, "Material 3", "Wear OS Expressive", choice);
        column.addView(choices);

        column.addView(Ui.body(this,
            "Automatic uses One UI on Samsung watches and Material 3 Expressive on others.",
            theme.textSecondary));
    }

    private void option(LinearLayout group, String id, String name, String detail, String choice) {
        boolean chosen = id.equals(choice);
        Ui.addRow(group, Ui.choice(this, Icons.STYLE, Ui.theme(this).neutral, name, detail, chosen,
            () -> {
                if (chosen) return;
                Theme.choose(this, id);
                build();
            }));
    }

    @Override
    public void finish() {
        super.finish();
        Ui.leave(this);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if (column != null && Ui.bezelScroll(column, ev)) return true;
        return super.dispatchGenericMotionEvent(ev);
    }
}
