package com.example.mycooking.view;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.mycooking.R;
import com.example.mycooking.activity.BreakfestActivity;
import com.example.mycooking.activity.MenuRankActivity;
import com.example.mycooking.activity.MenuSortActivity;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.Date;

/**
 * Created by Administrator on 2016/9/2.
 */
// 由于ScrollView 只允许有一个 one direct ChildView,所以再用LinearLayout来做容器
public class RefreshScLinearLayout extends LinearLayout {

    public static final String TAG = "Refresh";

    public Context context;
    private static ScrollView scrollView;
    private LinearLayout subLayout;
    private View refreshheader;
    private int headerHeight;//头高度
    private int lastHeaderPadding;//最后一次调用Move Header的padding

    //状态机
    static final private int RELEASE_To_REFRESH = 0;
    static final private int PULL_To_REFRESH = 1;
    static final private int REFRESHING = 2;
    static final private int DONE = 3;
    private int current_State = DONE;
    private boolean isBack; //从Release 转到 pull
    private RefreshListener listener;

    private ProgressBar pb_refreshheader_loading;
    private ImageView iv_refreshheader_arrow;
    private TextView tv_refreshheader_tips;
    private TextView tv_refreshheader_lastupdate;
    private RotateAnimation rotateAnimation;
    private RotateAnimation reverseAnimation;
    private TextView textView;
    private LayoutInflater layoutInflater;
    private View scChildView;
    private ImageButton bt_suggestpage_sort;
    private Button bt_suggestpage_rank;
    private ImageButton bt_suggestpage_breakfest;


    public RefreshScLinearLayout(Context context) {
        super(context);
        initView(context);
    }
    public RefreshScLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initView(context);
    }

    //在LinearLayout中放scrollView和refreshHeader
    public void initScrollView(final Context context){

        scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        //给scrollView增加子控件
        initScrollChildView(context);

        scrollView.addView(subLayout);

        //为ScrollView绑定滑动事件
        touchScrollView();
    }

    private void initScrollChildView(Context context) {
        //scrollView只有one direct childView, 所以再用LinearLayout来做容器
        subLayout = new LinearLayout(context);//改成用自己写的布局填充scrollview的childview
        subLayout.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        subLayout.setOrientation(VERTICAL);

        scChildView = View.inflate(context, R.layout.myscrollview, null);
        //给scrollview中的button设置点击事件
        //有问题--------------scButtonClickFunction(context);

        //给LinearLayout中增加 子view
       /* TextView textView = new TextView(context);
        textView.setText("测试scrollview的linearLayout的子view");*/
        subLayout.addView(scChildView);
        scButtonClickFunction(context);
        //subLayout.addView(textView);

    }

    private void scButtonClickFunction(final Context context) {

        Log.i(TAG,"ButtonClick");
        bt_suggestpage_sort = (ImageButton) scChildView.findViewById(R.id.bt_suggestpage_sort);
        Log.i(TAG,bt_suggestpage_sort.toString());
        bt_suggestpage_breakfest = (ImageButton) scChildView.findViewById(R.id.bt_suggestpage_breakfest);
        bt_suggestpage_rank = (Button) scChildView.findViewById(R.id.bt_suggestpage_Rank);


        CirclePageIndicator indicator_suggestmeal = (CirclePageIndicator) scChildView.findViewById(R.id.indicator_suggestmeal);
        ViewPager vp_suggestPage_meal = (ViewPager) scChildView.findViewById(R.id.vp_suggestPage_meal);
        vp_suggestPage_meal.setAdapter(new MysuggestmealPagerAdapter());

        indicator_suggestmeal.setViewPager(vp_suggestPage_meal);
        Log.i(TAG,indicator_suggestmeal.toString());


        //菜谱分类页面
        bt_suggestpage_sort.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, MenuSortActivity.class);
                context.startActivity(intent);
            }
        });

        //早餐页面
        bt_suggestpage_breakfest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startActivity(new Intent(context,BreakfestActivity.class));
            }
        });

        //排行榜页面
        Log.i(TAG, bt_suggestpage_rank.toString());
        bt_suggestpage_rank.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, MenuRankActivity.class);
                context.startActivity(intent);
            }
        });
    }

    public void initHeaderView(Context context){
        //---------------------refreshHeader
        refreshheader = layoutInflater.inflate(R.layout.refreshheader, null);
        //由于在onCreate()中拿不到refreshheader的高度，所以需要手动计算
        measureView(refreshheader);
        headerHeight = refreshheader.getMeasuredHeight();
        //最后一次调用Move Header时的padding
        lastHeaderPadding = (-1*headerHeight);
        //先将refresh头部隐藏
        refreshheader.setPadding(0,lastHeaderPadding,0,0);
        refreshheader.invalidate();

        //-------------------------给refreshHeader设置下拉刷新动画
        pb_refreshheader_loading = (ProgressBar) refreshheader.findViewById(R.id.pb_refreshheader_loading);
        iv_refreshheader_arrow = (ImageView) refreshheader.findViewById(R.id.iv_refreshheader_arrow);
        //iv_refreshheader_arrow.setMinimumHeight(50);
        // iv_refreshheader_arrow.setMinimumWidth(50);
        tv_refreshheader_tips = (TextView) refreshheader.findViewById(R.id.tv_refreshheader_tips);
        tv_refreshheader_lastupdate = (TextView)refreshheader.findViewById(R.id.tv_refreshheader_lastupdate);

        //下拉刷新时的动画
        refreshanimation();
    }

    public void initView(Context ctx){

        layoutInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //初始化scrollView的布局
        initScrollView(ctx);

        initHeaderView(ctx);

        this.addView(refreshheader,0);
        this.addView(scrollView,1);

    }

    private void refreshanimation() {
        //1、箭头旋转动画
        rotateAnimation = new RotateAnimation(0, 180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(200);//动画持续时间
        rotateAnimation.setFillAfter(true);//动画结束后保持动画的最后一帧
        //2、箭头反转动画
        reverseAnimation = new RotateAnimation(180, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        reverseAnimation.setDuration(200);
        reverseAnimation.setFillAfter(true);
    }

    //由于在onCreate()中拿不到refreshheader的高度，所以需要手动计算高度
    private void measureView(View childView){
        ViewGroup.LayoutParams layoutParams = childView.getLayoutParams();
        if (layoutParams==null){
            layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        //childDimension How big the child wants to be in the current dimension(尺寸);
        int childwidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, layoutParams.width);
        //测量view的高度
        int height = layoutParams.height;
        int childHeightSpec;

        if (height>0){
            childHeightSpec = MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY);
        }else{
            childHeightSpec = MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED);
        }

        childView.measure(childwidthSpec,childHeightSpec);
    }

    float startX;
    float startY;
   /* float endX;
    float endY;*/
    //为ScrollView绑定滑动事件
    private void touchScrollView(){

        scrollView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
               /* private final int INITSTATE    = 0;
                private final int NEED_RELEASE = 1;
                private final int REFRESHING   = 2;
                private int current_state = INITSTATE;*/
                switch (event.getAction()){

                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();

                        Log.i(TAG,"down"+":"+startX+"--"+startY);

                        break;
                    case MotionEvent.ACTION_MOVE:

                        if (startX ==0)
                            startX = event.getRawX();
                        if (startY ==0)
                            startY = event.getRawY();


                        //sc.getScrollY == 0  scrollview 滑动到头了
                        //lastHeaderPadding > (-1*headerHeight) 表示header还没完全隐藏起来时
                        //headerState != REFRESHING 当正在刷新时
                        if((scrollView.getScrollY() == 0 || lastHeaderPadding > (-1*headerHeight)) && current_State != REFRESHING) {
                            //拿到滑动的Y轴距离
                            int interval = (int) (event.getRawY() - startY);//原来的是-----event.getY() - startY
                            //是向下滑动而不是向上滑动
                            if (interval > 0) {
                                interval = interval/2;//下滑阻力

                                lastHeaderPadding = interval + (-1*headerHeight);
                                refreshheader.setPadding(0, lastHeaderPadding, 0, 0);
                                if(lastHeaderPadding > 0) {
                                    //txView.setText("我要刷新咯");
                                    current_State = RELEASE_To_REFRESH;
                                    //是否已经更新了UI
                                    if(! isBack) {
                                        isBack = true;  //到了Release状态，如果往回滑动到了pull则启动动画
                                        changeHeaderViewByState();
                                    }
                                } else {
                                    current_State = PULL_To_REFRESH;
                                    changeHeaderViewByState();
                                    //txView.setText("看到我了哦");
                                    //sc.scrollTo(0, headerPadding);
                                }
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:

                        startX= 0;
                        startY = 0;
                        if (current_State != REFRESHING) {
                            switch (current_State) {
                                case DONE:
                                    //什么也不干
                                    break;
                                case PULL_To_REFRESH:
                                    current_State = DONE;
                                    lastHeaderPadding = -1*headerHeight;
                                    refreshheader.setPadding(0, lastHeaderPadding, 0, 0);
                                    changeHeaderViewByState();
                                    break;
                                case RELEASE_To_REFRESH:
                                    isBack = false; //准备开始刷新，此时将不会往回滑动
                                    current_State = REFRESHING;
                                    changeHeaderViewByState();
                                    onRefresh();
                                    break;
                                default:
                                    break;
                            }
                        }
                        break;
                }

                //—————————————————滑动事件的返回值
                //如果Header是完全被隐藏的则让ScrollView正常滑动，让事件继续否则的话就阻断事件
                if(lastHeaderPadding > (-1*headerHeight) && current_State != REFRESHING) {
                    return true;
                } else {
                    return false;
                }

            }
        });
    }

//    @Override
//    public void addView(View child, int index, ViewGroup.LayoutParams params) {
//        //读取XML中的默认给 -1 自己添加的为0 和 1
//
//        if(index==-1){
//            subLayout.addView(child,params);
//            return;
//        }
//        super.addView(child, index, params);
//    }

    //外界调用来重写 刷新时，要在后台子线程中做的事情
    public void setRefreshListener(RefreshListener listener){
        this.listener = listener;
    }

    private void changeHeaderViewByState(){

    //三种状态 下拉刷新 松开刷新数据 正在加载
        switch (current_State){
            case PULL_To_REFRESH:
                // 是由RELEASE_To_REFRESH状态转变来的
                if (isBack) {
                    isBack = false;
                    iv_refreshheader_arrow.startAnimation(reverseAnimation);
                    //tv_refreshheader_tips.setText("下拉刷新");
                }
                tv_refreshheader_tips.setText("下拉刷新");
                break;
            case RELEASE_To_REFRESH:
                iv_refreshheader_arrow.setVisibility(View.VISIBLE);
                pb_refreshheader_loading.setVisibility(View.GONE);
                tv_refreshheader_tips.setVisibility(View.VISIBLE);
                tv_refreshheader_lastupdate.setVisibility(View.VISIBLE);
                iv_refreshheader_arrow.clearAnimation();
                iv_refreshheader_arrow.startAnimation(rotateAnimation);
                tv_refreshheader_tips.setText("松开刷新");
                break;
            case REFRESHING:
                lastHeaderPadding = 0;
                refreshheader.setPadding(0, lastHeaderPadding, 0, 0);
                refreshheader.invalidate();
                pb_refreshheader_loading.setVisibility(View.VISIBLE);
                iv_refreshheader_arrow.clearAnimation();
                iv_refreshheader_arrow.setVisibility(View.INVISIBLE);
                tv_refreshheader_tips.setText("正在刷新...");
                tv_refreshheader_lastupdate.setVisibility(View.VISIBLE);
                break;
            case DONE:
                lastHeaderPadding = -1 * headerHeight;
                refreshheader.setPadding(0, lastHeaderPadding, 0, 0);
                refreshheader.invalidate();
                pb_refreshheader_loading.setVisibility(View.GONE);
                iv_refreshheader_arrow.clearAnimation();
                iv_refreshheader_arrow.setVisibility(View.VISIBLE);
                tv_refreshheader_tips.setText("下拉刷新");
                tv_refreshheader_lastupdate.setVisibility(View.VISIBLE);
                break;
            default:
                break;

        }
    }

    //正在刷新，起子线程处理
    private void onRefresh(){

        new AsyncTask<Void, Void, Void> (){
            protected Void doInBackground (Void...params){
                //当重写了接口中的抽象方法时，则执行在子线程中做的事情
                if (listener != null) {
                    listener.doInBackground();
                }
                return null;
            }

            @Override
            protected void onPostExecute (Void result){
                onRefreshComplete();
                if (listener != null) {
                    listener.complete();
                }
            }
        }.execute();
    }

    //接口：刷新时调用的回调函数
    public interface RefreshListener {
        public void doInBackground();
        public void complete();
    }

    //刷新完毕，让header隐藏起来
    public void onRefreshComplete() {
        current_State = DONE;
        tv_refreshheader_lastupdate.setText("最近更新:" + new Date().toLocaleString());
        changeHeaderViewByState();
    }

    //给Viewpager设置Adapter
    class MysuggestmealPagerAdapter extends PagerAdapter {

        String[] whichMeal = new String[]{"早餐","午餐","下午茶","晚餐","夜宵"};

        String[] mealInfo = new String[]{"早餐早餐早餐","午餐午餐午餐","下午茶下午茶下午茶","晚餐晚餐晚餐","夜宵夜宵夜宵"};
        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view==object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            View page = View.inflate(context, R.layout.viewpager_meal, null);
            TextView tv_vpMeal_meal = (TextView) page.findViewById(R.id.tv_vpMeal_meal);
            TextView tv_vpMeal_info = (TextView) page.findViewById(R.id.tv_vpMeal_info);

            tv_vpMeal_meal.setText(whichMeal[position]);
            tv_vpMeal_info.setText(mealInfo[position]);
            //将view放入容器中
            container.addView(page);
            //将object返回
            return page;//super.instantiateItem(container, position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {


            container.removeView((View) object);

        }
    }
    public static void fun(){
        scrollView.scrollTo(0,0);
    }
}
