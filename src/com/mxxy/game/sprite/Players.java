package com.mxxy.game.sprite;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.event.EventListenerList;

import org.w3c.dom.events.EventException;

import com.mxxy.game.astar.Searcher;
import com.mxxy.game.config.PlayerVO;
import com.mxxy.game.event.EventDispatcher;
import com.mxxy.game.event.IEventTask;
import com.mxxy.game.event.PlayerEvent;
import com.mxxy.game.listener.IPlayerListener;
import com.mxxy.game.utils.Constant;
import com.mxxy.game.utils.MP3Player;
import com.mxxy.game.utils.SpriteFactory;
import com.mxxy.game.widget.Animation;

/**
 * 游戏人物
 * 
 * @author ZAB 邮箱 ：624284779@qq.com
 */
public class Players implements IEventTask {
	/** 攻击*/
	public static final String STATE_ATTACK="attack";
	/** 施法*/
	public static final String STATE_MAGIC="magic";
	/** 站立 */
	public static final String STATE_STAND = "stand";
	/**防御*/
	public static final String STATE_DEFEND="defend";
	/**被击中*/
	public static final String STATA_HIT="hit";
	/**倒下*/
	public static final String STATE_DIE="die";
	/** 移动 */
	public static final String STATE_WALK = "walk";
	/** 哭泣 **/
	public static final String STATE_WEEP = "weep";
	/** 舞蹈 */
	public static final String STATE_DANCE = "dance";
	/** 发怒 */
	public static final String STATE_ANGRY = "angry";
	/** 跑去*/
	public static final String STATE_RUSHA="rusha";
	/** 跑回*/
	public static final String STATE_RUSHB="rushb";
	/** 人物坐骑站立 */
	public static final String STATE_MOUNT_STAND = "mountstand";
	/** 人物坐骑移动 */
	public static final String STATE_MOUNT_WALK = "mountwalk";
	/** 人物状态 */
	private String state;
	/** 人物坐标 */
	private int x, y;
	/** 人物 */
	private Sprite person;
	/** 阴影 */
	private Sprite shadow;
	/** 方向 */
	private int direction;
	/** 人物名字 */
	private String personName;
	/** 人物称谓 */
	private String describe;

	private String id;
	/** 人物标识如0001(代表逍遥生) */
	private String character;
	/** 姓名字体 */
	private Font nameFont;
	/** 鼠标是否悬停 */
	private boolean isHover;
	/** 是否显示坐骑 */
	private int sceneX, sceneY;
	/** 名字颜色 */
	private Color nameBackground;
	/** 移动锁 */
	private Object MOVE_LOCK = new Object();
	/** 当前的移动量[x,y] */
	private Point nextStep;
	/** 路径队列 */
	private Queue<Point> path = new ConcurrentLinkedQueue<Point>();
	/** 事件集合 */
	private EventListenerList listenerList = new EventListenerList();
	/** 搜索路径 */
	private Searcher searcher;
	/** 人物数据 */
	private PlayerVO data;
	/** 人物染色 */
	private int[] colorations = null;
	/** 获取8个方向三角正切 */
	private static double k1 = Math.tan(Math.PI / 8);

	private static double k2 = 3 * k1;
	/** 门派标识 1000人族,1001魔族,1002仙族 */
	private String schoolCharacter;
	/** 坐骑*/
	private Mount mMount;  
	/** 武器*/
	private Weapon mWeapon; 
	/** 矩形绘制*/
	public Rectangle rect;
	/**人物速度*/
	private int speed;
	
	private int hp;
	
//	private Animation onceEffect;

	public void setShadow(boolean isShadow) {
		this.shadow = isShadow ? SpriteFactory.loadShadow() : null;
	}

	public void setMount(Mount mMount) {
		this.mMount = mMount;
		setState(mMount!=null?STATE_MOUNT_STAND:STATE_STAND);
	}

	public void setWeapon(Weapon mWeapon) {
		this.mWeapon = mWeapon;
	}

	public void setCharacter(String character) {
		this.character = character;
		if (Integer.parseInt(character) >= 1 && Integer.parseInt(character) <= 4) {
			this.setSchoolCharacter("1000");
		} else if (Integer.parseInt(character) >= 5 && Integer.parseInt(character) <= 8) {
			this.setSchoolCharacter("1001");
		} else if (Integer.parseInt(character) >= 9 && Integer.parseInt(character) <= 12) {
			this.setSchoolCharacter("1002");
		}
	}

	public void setData(PlayerVO data) {
		this.data = data;
		this.setSpeed(data.getSpeed());
		this.setCharacter(data.getCharacter());
		this.setCharacter(data.getCharacter());
		this.setPersonName(data.getName());
		this.setWeapon(data.getmWeapon());
		this.setDescribe(data.getDescribe());
		this.setDirection(data.getDirection());
		this.setColorations(data.getColorations(), true);
		this.setSceneLocation(data.getSceneLocation());
		this.setState(data.getState());
		this.setNameBackground(Constant.PLAYER_NAME_COLOR);
	}

	public PlayerVO getData() {
		return data;
	}

	/**
	 * 获取人物坐标
	 * @return
	 */
	public Point getLocation() {
		return new Point(x, y);
	}

	public Sprite getPerson() {
		return person;
	}

	/**
	 * 设置人物坐标
	 * @param x
	 * @param y
	 */
	public void setLocation(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * 获取当前场景坐标
	 * @return
	 */
	public Point getSceneLocation() {
		return new Point(sceneX, sceneY);
	}

	/**
	 * 设置场景坐标
	 * @param point
	 */
	public void setSceneLocation(Point point) {
		this.sceneX = point.x;
		this.sceneY = point.y;
	}

	/**
	 * 绘制
	 * @param g
	 */
	public void draw(Graphics2D g, int x, int y) {
		g.setFont(nameFont);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		if (mMount!=null)
			mMount.draw(g, x, y);
		if (person != null)
			person.drawBitmap(g, x, y);
		if (shadow != null)
			shadow.drawBitmap(g, x, y);
		if (mMount==null) {
			if (mWeapon != null)
				mWeapon.draw(g, x, y);
		}
		int textY = y + 25;
		int texts = y + 43;
		Graphics2D g2d = (Graphics2D) g.create();
		if (personName != null && personName!=null) {
			int textX = x - g.getFontMetrics().stringWidth(personName) / 2;
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2d.setColor(getNameBackground());
			g2d.setColor(isHover ? Color.red : getNameBackground());
			g2d.drawString(personName, textX, describe == null ? textY : texts);
		}

		if (describe != null && personName!=null) {
			int textsX = x - g.getFontMetrics().stringWidth(describe) / 2;
			g2d.setColor(isHover ? Color.red : Constant.DESCRIBE_COLOR);
			g2d.drawString(describe, textsX, textY);
		}

//		if(this.onceEffect!=null) {
//			this.onceEffect.drawBitmap(g2d, x, y);
//		}
//		g.setColor(Color.RED);
//		if(index<3){
//			g.drawRect(300+100*index,90, rect.width, rect.height);
//		}else{
//			g.drawRect(300+100*(index-3),220, rect.width, rect.height);
//		}
		g2d.dispose();
	}

	public int index;

	/**
	 * 更具状态 实例NPC人物
	 * @param state
	 * @return
	 */
	private Sprite createPerson(String state) {
		String mountIndex = mMount!=null ? mMount.getMountCharacter() : "";
		String value = mountIndex.length() > 0 ? mountIndex + "/" : "";
		Sprite sprite = SpriteFactory.loadSprite("/shape/char/" + this.character + "/" + value + state + ".tcp",
				this.colorations);
		return sprite;
	}

	public void setSchoolCharacter(String schoolCharacter) {
		this.schoolCharacter = schoolCharacter;
	}
	/**
	 * 设置状态 实例 Person 对象
	 * @param state
	 */
	public void setState(String state) {
		if (state == null) {
			state = mMount!=null ? STATE_MOUNT_STAND : STATE_STAND;
		}
		if (this.state != state) {
			this.state = state;
			this.person = createPerson(state);
			this.person.setDirection(this.direction);
			this.person.resetFrames();
			if (mMount==null) {
				if(mWeapon != null){
					mWeapon.createSprite(this);
					mWeapon.setDirection(this.direction);
					mWeapon.resetFrames();
				}
			} else {
				mMount.createSprite(this);
				mMount.setDirection(this.direction);
				mMount.resetFrames();
			}
		}
	}

	/**
	 * 更新动画
	 * @param elapsedTime
	 */
	public void update(long elapsedTime) {
		if (shadow != null)
			shadow.update(elapsedTime);
		if (person != null)
			person.update(elapsedTime);
		if (mWeapon != null)
			mWeapon.update(elapsedTime);
		if (mMount != null)
			mMount.update(elapsedTime);
//		if (this.onceEffect != null) {
//			this.onceEffect.update(elapsedTime);
//			if (this.onceEffect.getRepeat() == 0) {
//				this.onceEffect = null;
//			}
//		}
	}

	/**
	 * 设置方向
	 * @param direction
	 */
	public void setDirection(int direction) {
		if (this.direction != direction) {
			this.direction = direction;
			if (person != null) {
				person.setDirection(direction);
				person.resetFrames();
			}
			if (mWeapon != null) {
				mWeapon.setDirection(direction);
				mWeapon.resetFrames();
			}
			if (mMount != null) {
				mMount.setDirection(direction);
				mMount.resetFrames();
			}
		} else {
			reviseDirenction();
		}
	}

	/**
	 * 修改方向
	 */
	private void reviseDirenction() {
		if (person != null) {
			person.setDirection(direction);
			if (mWeapon != null) {
				mWeapon.setDirection(direction);
				if(mWeapon.getSprite()!=null)
					mWeapon.getSprite().getCurrAnimation().setIndex(mWeapon.getSprite().getCurrAnimation().getCurrentFrameIndex());
			}
			if (mMount != null) {
				mMount.setDirection(direction);
				mMount.getSprite().getCurrAnimation().setIndex(mMount.getSprite().getCurrAnimation().getCurrentFrameIndex());
			}
		}
	}

	/**
	 * 修改人物动作 并修改坐标
	 * @param elapsedTime
	 */
	public void updateMovement(long elapsedTime) {
		this.setState(this.isMoving() ? mMount!=null ? STATE_MOUNT_WALK : STATE_WALK : this.state);
		if (this.isMoving()) {
			if (this.isMoveSceneCoordinate()) {
				prepareStep();
			} else {
				Point d = this.calculateIncrement(elapsedTime);
				if (d.x != 0 || d.y != 0) {
					setLocation(getX() + d.x, getY() + d.y);
					PlayerEvent evt = new PlayerEvent(this, PlayerEvent.MOVE);
					evt.setAttributes(PlayerEvent.MOVE_INCREMENT, d);
					fireEvent(evt);
				}
			}
		}
	}

	/**
	 * 事件处理
	 */
	@Override
	public boolean handleEvent(EventObject evt) throws EventException {
		if (evt instanceof PlayerEvent) {
			PlayerEvent playerEvt = (PlayerEvent) evt;
			handleEvent(playerEvt);
		}
		return false;
	}

	/**
	 * 人物事件处理
	 * @param event
	 */
	private void handleEvent(PlayerEvent event) {
		final IPlayerListener[] listeners = listenerList.getListeners(IPlayerListener.class);
		switch (event.getId()) {
		case PlayerEvent.STEP_OVER:
			for (IPlayerListener listener : listeners) {
				listener.stepOver(this);
			}
			break;
		case PlayerEvent.WALK:
			for (IPlayerListener listener : listeners) {
				listener.walk(event);
			}
			break;
		case PlayerEvent.MOVE:
			for (IPlayerListener listener : listeners) {
				listener.move(this, (Point) event.getAttributes(PlayerEvent.MOVE_INCREMENT));
			}
			break;
		case PlayerEvent.CLICK:
			for (IPlayerListener listener : listeners) {
				listener.click(event);
			}
			break;
		case PlayerEvent.TALK:
			for (IPlayerListener listener : listeners) {
				listener.talk(event);
			}
			break;

		default:
			break;
		}
	}

	/**
	 * 清除所有Player事件
	 * @param l
	 */
	public void removePlayerListener(IPlayerListener l) {
		listenerList.remove(IPlayerListener.class, l);
	}

	/**
	 * 添加事件
	 * @param scenePlayerHandler
	 */
	public void addPlayerListener(IPlayerListener l) {
		listenerList.add(IPlayerListener.class, l);
	}

	public int getListenerList() {
		return listenerList.getListenerCount();
	}

	public void setPath(List<Point> path) {
		this.path.clear();
		this.path.addAll(path);
		if ((path == null) || (path.isEmpty()))
			System.out.println("没有找到路径");
	}

	/** 继续当前方向移动 */
	private boolean movingOn = false;

	private boolean moving = false;

	private boolean directionMoving = false;

	/**
	 * 移动
	 */
	public void move() {
		synchronized (MOVE_LOCK) {
			this.prepareStep();
		}
	}

	/**
	 * 准备移动
	 */
	private void prepareStep() {
		synchronized (MOVE_LOCK) {
			this.nextStep = this.popPath();
			// 路径已经为空,停止移动
			if (this.nextStep == null) {
				if (this.movingOn) {
					this.stepTo(direction); // 移动到
				} else {
					this.stopAction();
				}
			}
			this.stepAction();
		}
	}

	/**
	 * 正在移动
	 */
	private void stepAction() {
		if (this.nextStep != null) {
			this.moving = true;
			int dir = calculateStepDirection(this.nextStep);
			setDirection(dir);
		}
	}

	/**
	 * 根据路径的步进量计算出移动方向
	 * @param step
	 * @return
	 */
	private int calculateStepDirection(Point step) {
		int dx = step.x - this.sceneX;
		int dy = step.y - this.sceneY;
		double r = Math.sqrt(dx * dx + dy * dy);
		double cos = dx / r;
		int angle = (int) Math.floor(Math.acos(cos) * 180 / Math.PI);
		if (dy > 0) {
			angle = 360 - angle;
		}
		int dir = 0;
		if (angle > 337 || angle <= 22) {
			dir = Sprite.DIRECTION_RIGHT;
		} else if (angle > 22 && angle <= 67) {
			dir = Sprite.DIRECTION_BOTTOM_RIGHT;
		} else if (angle > 67 && angle <= 112) {
			dir = Sprite.DIRECTION_BOTTOM;
		} else if (angle > 112 && angle <= 157) {
			dir = Sprite.DIRECTION_BOTTOM_LEFT;
		} else if (angle > 157 && angle <= 202) {
			dir = Sprite.DIRECTION_LEFT;
		} else if (angle > 202 && angle <= 247) {
			dir = Sprite.DIRECTION_TOP_LEFT;
		} else if (angle > 247 && angle <= 292) {
			dir = Sprite.DIRECTION_TOP;
		} else if (angle > 292 && angle <= 337) {
			dir = Sprite.DIRECTION_TOP_RIGHT;
		}
		return dir;
	}

	/**
	 * 停止移动
	 */
	private void stopAction() {
		synchronized (MOVE_LOCK) {
			this.moving = false;
			this.movingOn = false;
			this.setState(mMount!=null ? STATE_MOUNT_STAND : STATE_STAND);
		}
	}

	private Point popPath() {
		if (this.path != null && !this.path.isEmpty()) {
			Point step = this.path.poll();
			while (step != null && step.x == this.sceneX && step.y == this.sceneY) {
				step = this.path.poll();
			}
			return step;
		}
		return null;
	}


	/**
	 * 根据方向移动到指定路径
	 * @param direction
	 */
	private void stepTo(int direction) {
		this.clearPath();
		int dx = 0;
		int dy = 0;
		switch (direction) {
		case Sprite.DIRECTION_LEFT:
			dx = -1;
			break;
		case Sprite.DIRECTION_TOP:
			dy = 1;
			break;
		case Sprite.DIRECTION_RIGHT:
			dx = 1;
			break;
		case Sprite.DIRECTION_BOTTOM:
			dy = -1;
			break;
		case Sprite.DIRECTION_BOTTOM_LEFT:
			dx = -1;
			dy = -1;
			break;
		case Sprite.DIRECTION_TOP_LEFT:
			dx = -1;
			dy = 1;
			break;
		case Sprite.DIRECTION_TOP_RIGHT:
			dx = -1;
			dy = 1;
			break;
		case Sprite.DIRECTION_BOTTOM_RIGHT:
			dx = 1;
			dy = -1;
			break;
		default:
			break;
		}
		Point next = new Point(this.sceneX + dx, this.sceneY + dy);
		this.addStep(next);
		this.prepareStep();
	}

	private void addStep(Point next) {
		this.path.add(next);
	}

	public void clearPath() {
		this.path.clear();
	}

	public boolean isMoving() {
		return moving;
	}

	public void moveOn() {
		this.movingOn = true;
	}

	public void stop(boolean force) {
		synchronized (MOVE_LOCK) {
			if (force) {
				stopAction();
			} else {
				this.movingOn = false;
			}
			this.directionMoving = false;
		}
	}

	/**
	 * 判断是否可以移动到 nextStop path
	 * 
	 * @return
	 */
	private boolean isMoveSceneCoordinate() {
		return getSceneLocation().equals(nextStep);
	}

	/**
	 * 计算
	 * @param elapsedTime
	 * @return
	 */
	private Point calculateIncrement(long elapsedTime) {
		int dx = 0, dy = 0;
		if (searcher.pass(this.nextStep.x, this.nextStep.y)) {
			// 计算起点与目标点的弧度角
			double radian = Math.atan(1.0 * (nextStep.y - sceneY) / (nextStep.x - sceneX));
			// 计算移动量
			int distance = (int) (Constant.PLAYER_SPEED * elapsedTime);

			dx = (int) (distance * Math.cos(radian));
			dy = (int) (distance * Math.sin(radian));
			if (nextStep.x > sceneX) {
				dx = Math.abs(dx);
			} else {
				dx = -Math.abs(dx);
			}
			if (nextStep.y > sceneY) {
				dy = -Math.abs(dy);
			} else {
				dy = Math.abs(dy);
			}
		} else if (!this.directionMoving) {
			stopAction();
		}
		return new Point(dx, dy);
	}

	/**
	 * 事件消费
	 * @param e
	 */
	@SuppressWarnings("unchecked")
	public void fireEvent(PlayerEvent e) {
		EventDispatcher.getInstance().dispatchEvent(e);
	}

	/**
	 * 修改方向
	 * @param mouse
	 */
	public void changeDirection(Point mouse) {
		int direction =  calculateStepDirection(mouse);
		setDirection(direction);
	}

	/**
	 * @param src
	 * @param mouse
	 * @return
	 */
	public int computeDirection(Point src, Point mouse) {
		double dy, dx, k;
		int direction = Sprite.DIRECTION_BOTTOM_RIGHT;
		dy = mouse.y - src.y;
		dx = mouse.x - src.x;
		if (dx == 0) {
			return (dy >= 0) ? Sprite.DIRECTION_BOTTOM : Sprite.DIRECTION_TOP;
		} else if (dy == 0) {
			return (dx >= 0) ? Sprite.DIRECTION_RIGHT : Sprite.DIRECTION_LEFT;
		}
		k = Math.abs(dy / dx);
		if (k >= k2) {
			if (dy > 0)
				direction = Sprite.DIRECTION_BOTTOM;
			else
				direction = Sprite.DIRECTION_TOP;
		} else if (k <= k1) {
			if (dx > 0)
				direction = Sprite.DIRECTION_RIGHT;
			else
				direction = Sprite.DIRECTION_LEFT;
		} else if (dy > 0) {
			if (dx > 0)
				direction = Sprite.DIRECTION_BOTTOM_RIGHT;
			else
				direction = Sprite.DIRECTION_BOTTOM_LEFT;
		} else {
			if (dx > 0)
				direction = Sprite.DIRECTION_TOP_RIGHT;
			else
				direction = Sprite.DIRECTION_TOP_LEFT;
		}
		return direction;
	}


	public boolean contains(int x, int y) {
		if(person!=null && shadow!=null) {
			boolean b = person.contains(x, y) || shadow.contains(x, y);
			if ((this.mWeapon!=null&&this.mWeapon.getSprite() != null) && (!b)) {
				b = this.mWeapon.getSprite().contains(x, y);
			}
			return b;
		}
		return false;
	}

	public void setColorations(int[] colorations, boolean recreate) {
		this.colorations = colorations;
		if (recreate)
			coloring(colorations);
	}

	/**
	 * 改色后的 人物角色
	 * @param colorations
	 */
	public void coloring(int[] colorations) {
		if (person != null) {
			this.person = createPerson(this.state);
			this.person.setDirection(this.direction);
			this.person.resetFrames();
		}

		if(mWeapon!=null){
			mWeapon.createSprite(this);
			mWeapon.setDirection(this.direction);
			mWeapon.resetFrames();
		}
	}

	/**
	 * 播放指定动作
	 * @param state
	 */
	public void playOnce(String state){
		this.setState(state);
		this.person.setRepeat(1);
		if(this.mWeapon!=null){
			this.mWeapon.getSprite().setRepeat(1);
		}
	}
	
	public void writFor(){
		this.person.getCurrAnimation().waitFor();
	}


//	public void playEffect(String name, boolean sound) {
//		Animation s = SpriteFactory.loadAnimation("/magic/"+"1002"+"/"+ name + ".tcp");
//		s.setRepeat(1);
//		this.onceEffect = s;
//		if(sound) {   
//			try {
//				MP3Player.play("sound/magic/" + name + ".mp3");
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//	}

	public void removeAllListeners() {
		IPlayerListener[] listeners = listenerList.getListeners(IPlayerListener.class);
		for (int i = 0; i < listeners.length; i++) {
			this.removePlayerListener(listeners[i]);
		}
	}

	public void moveBy(int dx, int dy) {
		this.x += dx;
		this.y += dy;
	}

	/**
	 * 获取文件名如0001
	 * @return
	 */
	public String getCharacter() {
		return character;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setNameBackground(Color nameBackground) {
		this.nameBackground = nameBackground;
	}

	public Color getNameBackground() {
		return nameBackground;
	}

	public void setHover(boolean isHover) {
		this.isHover = isHover;
	}

	public boolean isHover() {
		return isHover;
	}

	public int[] getColorations() {
		return colorations;
	}

	public void setDescribe(String describe) {
		this.describe = describe;
	}

	public String getState() {
		return state;
	}

	public void setPersonName(String personName) {
		this.personName = personName;
	}

	public String getDescribe() {
		return describe;
	}

	public int getDirection() {
		return direction;
	}

	public Mount getMount() {
		return mMount;
	}

	public String getPersonName() {
		return personName;
	}

	public void setSearcher(Searcher searcher) {
		this.searcher = searcher;
	}

	public void setRect(Rectangle rect) {
		this.rect = rect;
	}

	public Searcher getSearcher() {
		return searcher;
	}

	public String getSchoolCharacter() {
		return this.schoolCharacter;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
	
	public int getSpeed() {
		return speed;
	}
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	
	public void setHp(int hp) {
		this.hp = hp;
	}
	
	public int getHp() {
		return hp;
	}
	
	@Override
	public String toString() {
		return "Players [state=" + state + ", x=" + x + ", y=" + y + ", 方向=" + direction +",速度"+ speed +", 人物名字=" + personName
				+ ", describe=" + describe + ", character=" + character + ",sceneX=" + sceneX + ", sceneY=" + sceneY + ", 改色方案="
				+ Arrays.toString(colorations) + "]";
	}
}
