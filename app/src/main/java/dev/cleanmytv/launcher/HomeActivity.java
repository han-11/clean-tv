package dev.cleanmytv.launcher;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.tv.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import java.util.*;

/** Offline TV home: no accessibility helpers, background services, or network polling. */
public final class HomeActivity extends Activity {
    final ArrayList<App> apps = new ArrayList<>();
    final ArrayList<View> topButtons = new ArrayList<>();
    RecyclerView grid;
    GridLayoutManager layout;
    FrameLayout stage;
    View highlight;
    TextView title;
    View glow;
    int columns, dockCount, lastIndex = 0;
    String lastPackage = "";
    static final class App {
        final String pkg, label; final Intent intent; final Drawable image;
        App(String p, String l, Intent i, Drawable d) { pkg=p; label=l; intent=i; image=d; }
    }
    int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    TextView text(String value, float size) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(Color.WHITE);
        t.setFontFeatureSettings("kern"); return t;
    }
    GradientDrawable rounded(int color, float radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        columns=Math.max(4,Math.min(7,getResources().getInteger(R.integer.columns)));
        lastPackage=getPreferences(0).getString("lastPackage", "");
        loadApps();
        stage=new FrameLayout(this); stage.setBackgroundColor(Color.BLACK); setContentView(stage);
        try (java.io.InputStream input=getAssets().open("wallpaper.jpg")) {
            Bitmap photo=BitmapFactory.decodeStream(input);
            if(photo==null) throw new java.io.IOException("Unreadable wallpaper");
            ImageView wallpaper=new ImageView(this);
            wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
            wallpaper.setImageBitmap(photo);
            wallpaper.setFocusable(false);
            stage.addView(wallpaper,new FrameLayout.LayoutParams(-1,-1));
            View shade=new View(this);
            shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x77000000,0x33000000,0xaa000000}));
            stage.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        } catch(java.io.IOException failure) {
            Toast.makeText(this,"Wallpaper unavailable; using black",Toast.LENGTH_LONG).show();
        }

        grid=new RecyclerView(this); grid.setClipToPadding(false); grid.setPadding(dp(36),dp(12),dp(36),dp(36));
        layout=new GridLayoutManager(this,columns);
        layout.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() { public int getSpanSize(int position) { return position==0?columns:1; } });
        grid.setLayoutManager(layout); grid.setItemAnimator(null); grid.setAdapter(new Tiles());
        stage.addView(grid,new FrameLayout.LayoutParams(-1,-1));
        highlight=new View(this);
        GradientDrawable glass=rounded(Color.TRANSPARENT,13); glass.setStroke(dp(2),0xaaffffff);
        highlight.setBackground(glass); highlight.setVisibility(View.INVISIBLE); highlight.setFocusable(false);
        stage.addView(highlight,new FrameLayout.LayoutParams(1,1));
        grid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(RecyclerView r,int x,int y) { View f=getCurrentFocus(); if(f!=null) moveHighlight(f,false); }
        });
        for(int i=0;i<apps.size();i++) if(apps.get(i).pkg.equals(lastPackage)) lastIndex=i;
        grid.post(() -> focusApp(lastIndex));
    }
    void loadApps() {
        PackageManager pm=getPackageManager();
        TreeMap<String,App> available=new TreeMap<>();
        for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}) {
            Intent query=new Intent(Intent.ACTION_MAIN).addCategory(category);
            for(ResolveInfo r:pm.queryIntentActivities(query,0)) {
                String p=r.activityInfo.packageName;
                if(p.equals(getPackageName()) || p.equals("dev.cleanmytv.probe") || p.equals("com.google.android.apps.tv.launcherx")) continue;
                // Never expose factory/service menus as launchable tiles.
                String c=(p+"/"+r.activityInfo.name).toLowerCase(Locale.ROOT);
                if(c.contains("factory")||c.contains("hotelmenu")||c.contains("production")||c.contains("fac.menu")) continue;
                if(available.containsKey(p)) continue;
                Drawable image=null;
                try { image=pm.getActivityBanner(new ComponentName(p,r.activityInfo.name)); } catch(Exception ignored) {}
                if(image==null) image=r.loadIcon(pm);
                Intent launch=new Intent(Intent.ACTION_MAIN).addCategory(category).setComponent(new ComponentName(p,r.activityInfo.name));
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                available.put(p,new App(p,r.loadLabel(pm).toString(),launch,image));
            }
        }
        String[] dock=getResources().getStringArray(R.array.dock_apps), labels=getResources().getStringArray(R.array.dock_labels);
        dockCount=Math.min(columns,dock.length);
        for(int i=0;i<dockCount;i++) {
            App a=available.remove(dock[i]);
            if(a!=null) apps.add(new App(a.pkg,i<labels.length?labels[i]:a.label,a.intent,a.image));
            else apps.add(new App(dock[i],i<labels.length?labels[i]:dock[i],null,null));
        }
        ArrayList<App> rest=new ArrayList<>(available.values());
        rest.sort((a,b)->a.label.compareToIgnoreCase(b.label)); apps.addAll(rest);
    }
    View header() {
        FrameLayout h=new FrameLayout(this);
        // Header plus one tile row fills the initial viewport; Down reveals the remaining apps.
        int screenHeight=getResources().getDisplayMetrics().heightPixels;
        h.setLayoutParams(new RecyclerView.LayoutParams(-1,Math.max(dp(210),screenHeight-dp(180))));
        glow=new View(this); glow.setAlpha(0f);
        FrameLayout.LayoutParams gp=new FrameLayout.LayoutParams(dp(650),-1); gp.gravity=Gravity.CENTER_HORIZONTAL;
        h.addView(glow,gp);
        LinearLayout welcome=new LinearLayout(this); welcome.setOrientation(LinearLayout.VERTICAL);
        welcome.addView(text(getString(R.string.greeting),16));
        TextClock clock=new TextClock(this); clock.setFormat12Hour("h:mm"); clock.setFormat24Hour("HH:mm");
        clock.setTextColor(Color.WHITE); clock.setTextSize(54); clock.setTypeface(Typeface.create("sans-serif-thin",0)); welcome.addView(clock);
        TextClock date=new TextClock(this); date.setFormat12Hour("EEEE, d MMMM"); date.setFormat24Hour("EEEE, d MMMM");
        date.setTextColor(0xffb6b6bb); date.setTextSize(15); welcome.addView(date);
        FrameLayout.LayoutParams wp=new FrameLayout.LayoutParams(-2,-2); wp.topMargin=dp(20); h.addView(welcome,wp);
        LinearLayout actions=new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        addPill(actions,"Inputs",()->showInputs(topButtons.get(0)));
        addPill(actions,"Wi-Fi",()->open(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        addPill(actions,"Settings",()->open(new Intent(Settings.ACTION_SETTINGS)));
        FrameLayout.LayoutParams ap=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.RIGHT); ap.topMargin=dp(20); h.addView(actions,ap);
        title=text(apps.isEmpty()?"Your apps":apps.get(Math.min(lastIndex,apps.size()-1)).label,44);
        title.setTypeface(Typeface.create("sans-serif-light",0));
        FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM); tp.bottomMargin=dp(28); h.addView(title,tp);
        return h;
    }
    void addPill(LinearLayout row,String label,Runnable action) {
        final int index=topButtons.size();
        TextView button=text(label,14); button.setGravity(Gravity.CENTER); button.setPadding(dp(18),dp(10),dp(18),dp(10));
        button.setBackground(rounded(0x30ffffff,24)); button.setFocusable(true); button.setClickable(true);
        button.setOnClickListener(v->action.run()); button.setOnFocusChangeListener((v,focused)->{if(focused) moveHighlight(v,true);});
        button.setOnKeyListener((v,key,event)-> {
            if(event.getAction()!=KeyEvent.ACTION_DOWN) return false;
            if(key==KeyEvent.KEYCODE_DPAD_LEFT) { topButtons.get(Math.max(0,index-1)).requestFocus(); return true; }
            if(key==KeyEvent.KEYCODE_DPAD_RIGHT) { topButtons.get(Math.min(topButtons.size()-1,index+1)).requestFocus(); return true; }
            if(key==KeyEvent.KEYCODE_DPAD_UP) return true;
            if(key==KeyEvent.KEYCODE_DPAD_DOWN) { focusApp(Math.min(lastIndex,dockCount-1)); return true; }
            return false;
        });
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2); p.leftMargin=dp(10); row.addView(button,p); topButtons.add(button);
    }
    void open(Intent intent) { try { startActivity(intent); } catch(Exception e) { Toast.makeText(this,"Unavailable: "+e.getMessage(),Toast.LENGTH_LONG).show(); } }
    void showInputs(View anchor) {
        PopupMenu menu=new PopupMenu(this,anchor);
        try {
            TvInputManager manager=(TvInputManager)getSystemService(TV_INPUT_SERVICE);
            int added=0;
            for(TvInputInfo info:manager.getTvInputList()) {
                if(!info.isPassthroughInput()) continue;
                String label=info.loadLabel(this).toString();
                if(info.loadCustomLabel(this)!=null) label=info.loadCustomLabel(this).toString();
                final Uri uri=TvContract.buildChannelUriForPassthroughInput(info.getId());
                menu.getMenu().add(label).setOnMenuItemClickListener(item->{open(new Intent(Intent.ACTION_VIEW,uri));return true;}); added++;
            }
            menu.getMenu().add("Live TV").setOnMenuItemClickListener(item->{open(new Intent(Intent.ACTION_VIEW,TvContract.Channels.CONTENT_URI));return true;});
            if(added==0) Toast.makeText(this,"No external inputs were exposed by this firmware.",Toast.LENGTH_LONG).show();
            menu.show();
        } catch(Exception e) { Toast.makeText(this,"Input list unavailable; use the remote Input button.",Toast.LENGTH_LONG).show(); }
    }
    void focusApp(int index) {
        if(apps.isEmpty()) return;
        int target=Math.max(0,Math.min(index,apps.size()-1));
        View child=layout.findViewByPosition(target+1);
        if(child!=null) { child.requestFocus(); return; }
        layout.scrollToPositionWithOffset(target+1,dp(80));
        grid.post(()-> { View v=layout.findViewByPosition(target+1); if(v!=null) v.requestFocus(); });
    }
    void moveHighlight(View focused,boolean animate) {
        View target=focused;
        if(focused instanceof LinearLayout) {
            View first=((LinearLayout)focused).getChildAt(0);
            if(first instanceof ImageView) target=first;
        }
        if(target.getWidth()==0) return;
        int[] a=new int[2],b=new int[2];
        target.getLocationInWindow(a); stage.getLocationInWindow(b);
        int gap=dp(3);
        FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)highlight.getLayoutParams();
        p.width=target.getWidth()+gap*2;
        p.height=target.getHeight()+gap*2;
        highlight.setLayoutParams(p);
        float x=a[0]-b[0]-gap,y=a[1]-b[1]-gap;
        highlight.setVisibility(View.VISIBLE);
        highlight.animate().cancel();
        if(animate) highlight.animate().translationX(x).translationY(y).setDuration(220)
            .setInterpolator(new android.view.animation.OvershootInterpolator(.5f)).start();
        else {
            highlight.setTranslationX(x);
            highlight.setTranslationY(y);
        }
    }
    void ambient(Drawable image) {
        int color=0xff354858;
        if(image!=null) try {
            Bitmap sample=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888); Canvas canvas=new Canvas(sample);
            Rect bounds=new Rect(image.getBounds()); image.setBounds(0,0,8,8); image.draw(canvas); image.setBounds(bounds);
            long r=0,g=0,b=0; for(int y=0;y<8;y++) for(int x=0;x<8;x++) {int c=sample.getPixel(x,y);r+=Color.red(c);g+=Color.green(c);b+=Color.blue(c);}
            color=Color.rgb((int)r/64,(int)g/64,(int)b/64); sample.recycle();
        } catch(Exception ignored) {}
        if(glow!=null) { GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{Color.TRANSPARENT,color,Color.TRANSPARENT}); d.setGradientType(GradientDrawable.RADIAL_GRADIENT); d.setGradientRadius(dp(340)); glow.setBackground(d); }
    }
    final class Holder extends RecyclerView.ViewHolder { Holder(View v) {super(v);} }
    final class Tiles extends RecyclerView.Adapter<Holder> {
        @Override public int getItemCount() { return apps.size()+1; }
        @Override public int getItemViewType(int p) {return p==0?0:1;}
        @Override public Holder onCreateViewHolder(ViewGroup parent,int type) {
            if(type==0) return new Holder(header());
            LinearLayout tile=new LinearLayout(HomeActivity.this); tile.setOrientation(LinearLayout.VERTICAL); tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(8),dp(8),dp(8),dp(8)); tile.setFocusable(true); tile.setClickable(true);
            RecyclerView.LayoutParams tileParams=new RecyclerView.LayoutParams(-1,dp(104));
            tileParams.setMargins(dp(6),dp(6),dp(6),dp(6));
            tile.setLayoutParams(tileParams);

            ImageView image=new ImageView(HomeActivity.this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View view,Outline outline) {
                    outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),dp(10));
                }
            });
            image.setClipToOutline(true);

            tile.addView(image,new LinearLayout.LayoutParams(dp(144),dp(81)));
            TextView label=text("",13); label.setSingleLine(true); label.setEllipsize(android.text.TextUtils.TruncateAt.END); label.setGravity(Gravity.CENTER);
            tile.addView(label,new LinearLayout.LayoutParams(-1,dp(28)));
            return new Holder(tile);
        }
        @Override public void onBindViewHolder(Holder h,int position) {
            if(position==0) return;
            int index=position-1; App app=apps.get(index); LinearLayout tile=(LinearLayout)h.itemView;
            ImageView image=(ImageView)tile.getChildAt(0); TextView label=(TextView)tile.getChildAt(1);
            image.setImageDrawable(app.image); label.setText(app.label); label.setVisibility(View.GONE);
            tile.setContentDescription(app.label); tile.setBackground(null);
            tile.setOnClickListener(v->{ if(app.intent!=null) open(app.intent); else Toast.makeText(HomeActivity.this,app.label+" is not installed or not visible",Toast.LENGTH_LONG).show(); });
            tile.setOnFocusChangeListener((v,focused)-> {
                image.setScaleX(1f); image.setScaleY(1f); label.setAlpha(focused?1f:.65f);
                if(focused) {
                    lastIndex=index; lastPackage=app.pkg; getPreferences(0).edit().putString("lastPackage",lastPackage).apply();
                    if(title!=null && index<dockCount) { title.setText(app.label); ambient(app.image); }
                    v.post(()->moveHighlight(v,true));
                }
            });
            tile.setOnKeyListener((v,key,event)-> {
                if(event.getAction()!=KeyEvent.ACTION_DOWN) return false;
                if(key==KeyEvent.KEYCODE_DPAD_UP) { if(index<columns) {layout.scrollToPositionWithOffset(0,0);grid.post(()->{if(!topButtons.isEmpty())topButtons.get(0).requestFocus();});} else focusApp(index-columns); return true; }
                if(key==KeyEvent.KEYCODE_DPAD_DOWN) {if(index+columns<apps.size())focusApp(index+columns);return true;}
                if(key==KeyEvent.KEYCODE_DPAD_LEFT) {if(index%columns>0)focusApp(index-1);return true;}
                if(key==KeyEvent.KEYCODE_DPAD_RIGHT) {if(index%columns<columns-1 && index+1<apps.size())focusApp(index+1);return true;}
                return false;
            });
        }
    }
    @Override public void onBackPressed() { focusApp(0); }
}
