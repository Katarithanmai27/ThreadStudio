import React, { useEffect, useMemo, useState } from 'react';
import {
  ShoppingBag, Search, Heart, User, Menu, Plus,
  PackageCheck, LogOut, ArrowRight, Shirt, Trash2
} from 'lucide-react';

const API = (import.meta.env.VITE_API_URL || 'http://localhost:8081/api').replace(/\/$/, '');

const demo = [
  {id:101,title:'Rose Garden Co-ord',category:'Contemporary',price:2499,size:'S, M, L',imageUrl:'https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?auto=format&fit=crop&w=900&q=85',description:'A soft statement set designed for slow mornings and long evenings.',designer:{name:'Studio Aira'},stock:5},
  {id:102,title:'Indigo Handloom Dress',category:'Ethnic',price:3299,size:'S, M, L, XL',imageUrl:'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=900&q=85',description:'A modern silhouette with handcrafted details.',designer:{name:'Mira Creates'},stock:4},
  {id:103,title:'Sculpted Evening Top',category:'Occasion',price:1899,size:'XS, S, M',imageUrl:'https://images.unsplash.com/photo-1551163943-3f6a855d1153?auto=format&fit=crop&w=900&q=85',description:'Sculptural lines and a confident, clean finish.',designer:{name:'Label Nila'},stock:6},
  {id:104,title:'Everyday Linen Set',category:'Minimal',price:2799,size:'S, M, L, XL',imageUrl:'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=900&q=85',description:'Breathable linen-inspired styling for everyday wear.',designer:{name:'The Loom Room'},stock:3},
  {id:105,title:'Midnight Draped Saree',category:'Ethnic',price:4599,size:'Free size',imageUrl:'https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?auto=format&fit=crop&w=900&q=85',description:'A contemporary drape made for celebrations.',designer:{name:'Aarohi Studio'},stock:2},
  {id:106,title:'Soft Form Jacket',category:'Contemporary',price:3599,size:'S, M, L',imageUrl:'https://images.unsplash.com/photo-1548126032-079a0fb0099d?auto=format&fit=crop&w=900&q=85',description:'An expressive layer with a tailored shape.',designer:{name:'Studio Aira'},stock:2}
];

const money = n => '₹' + Number(n || 0).toLocaleString('en-IN');

export default function App() {

  const [products,setProducts] = useState(demo);
  const [query,setQuery] = useState('');
  const [category,setCategory] = useState('All');
  const [cart,setCart] = useState([]);
  const [page,setPage] = useState('shop');
  const [token,setToken] = useState(localStorage.getItem('ts_token') || '');
  const [user,setUser] = useState(JSON.parse(localStorage.getItem('ts_user') || 'null'));
  const [authMode,setAuthMode] = useState('login');
  const [notice,setNotice] = useState('');
  const [selected,setSelected] = useState(null);
  const [auth,setAuth] = useState({name:'',email:'',password:'',role:'USER'});
  const [form,setForm] = useState({title:'',description:'',price:'',category:'Contemporary',imageUrl:'',size:'S, M, L',stock:1});
  const [address,setAddress] = useState('');
  const [orders,setOrders] = useState([]);
  const [myProducts,setMyProducts] = useState([]);
  const [adminProducts,setAdminProducts] = useState([]);
  const [designers,setDesigners] = useState([]);

  // Added payment selection
  const [paymentMethod, setPaymentMethod] = useState('COD');

  const headers = token
    ? {'Content-Type':'application/json',Authorization:`Bearer ${token}`}
    : {'Content-Type':'application/json'};

  async function api(path,opts={}) {
    const r = await fetch(API + path, {
      ...opts,
      headers:{...headers,...opts.headers}
    });

    const data = r.status === 204 ? null : await r.json().catch(() => ({}));

    if (!r.ok) throw new Error(data?.message || `Request failed (${r.status})`);

    return data;
  }

  async function loadProducts() {
    try {
      const data = await api('/products');
      setProducts(data);
    } catch (error) {
      console.error('Failed to fetch products:', error);
      setProducts([]);
      flash('Unable to load products from backend');
    }
  }

  useEffect(() => {
    loadProducts();
  }, []);

  useEffect(() => {
    if (!token) return;

    if (page === 'orders')
      api('/orders').then(setOrders).catch(e => flash(e.message));

    if (page === 'designer') {
      api('/designer/products').then(setMyProducts).catch(() => {});
      api('/designer/orders').then(setOrders).catch(() => {});
    }

    if (page === 'admin') {
      api('/admin/products').then(setAdminProducts).catch(e => flash(e.message));
      api('/admin/designers').then(setDesigners).catch(() => {});
    }
  }, [page,token]);

  function flash(s) {
    setNotice(s);
    setTimeout(() => setNotice(''),3500);
  }

  const shown = useMemo(
    () => products.filter(p =>
      (category === 'All' || p.category === category) &&
      `${p.title} ${p.category} ${p.designer?.name || ''}`
        .toLowerCase().includes(query.toLowerCase())
    ),
    [products,category,query]
  );

  function add(p) {
    setCart(c =>
      c.some(x => x.id === p.id)
        ? c.map(x => x.id === p.id ? {...x,qty:x.qty+1} : x)
        : [...c,{...p,qty:1}]
    );
    flash('Added to your bag');
  }

  const total = cart.reduce((s,p) => s + p.price * p.qty,0);

  async function submitAuth(e) {
    e.preventDefault();
    try {
      const d = await api('/auth/' + (authMode === 'login' ? 'login' : 'register'), {
        method:'POST',
        body:JSON.stringify(auth)
      });

      setToken(d.token);
      setUser(d.user);
      localStorage.setItem('ts_token',d.token);
      localStorage.setItem('ts_user',JSON.stringify(d.user));

      setPage(d.user.role === 'ADMIN' ? 'admin' : d.user.role === 'DESIGNER' ? 'designer' : 'shop');
      flash('Welcome to ThreadStudio');
    } catch(e) {
      flash(e.message);
    }
  }

  function logout() {
    localStorage.removeItem('ts_token');
    localStorage.removeItem('ts_user');
    setToken('');
    setUser(null);
    setOrders([]);
    setMyProducts([]);
    setAdminProducts([]);
    setDesigners([]);
    setPage('shop');
    flash('Logged out successfully');
  }

  // Updated checkout: COD + Razorpay
  async function checkout() {
    if (!token) {
      setAuthMode('login');
      setPage('auth');
      flash('Sign in to place an order');
      return;
    }

    if (!cart.length) {
      flash('Your cart is empty');
      return;
    }

    if (!address.trim()) {
      flash('Enter a shipping address');
      return;
    }

    const productIds = cart.flatMap(p => Array(p.qty).fill(p.id));

    try {
      // Existing COD functionality preserved
      if (paymentMethod === 'COD') {
        await api('/orders', {
          method:'POST',
          body:JSON.stringify({
            shippingAddress:address,
            productIds
          })
        });

        setCart([]);
        setAddress('');
        setPage('orders');
        flash('Order placed successfully — cash on delivery');
        return;
      }

      // Load Razorpay script
      if (!window.Razorpay) {
        const script = document.createElement('script');
        script.src = 'https://checkout.razorpay.com/v1/checkout.js';
        script.async = true;

        document.body.appendChild(script);

        await new Promise((resolve,reject) => {
          script.onload = resolve;
          script.onerror = () => reject(new Error('Unable to load Razorpay'));
        });
      }

      // Create Razorpay order
      const order = await api('/payments/create-order', {
        method:'POST',
        body:JSON.stringify({
          shippingAddress:address,
          productIds
        })
      });

      const options = {
        key:order.keyId,
        amount:order.amount,
        currency:order.currency,
        name:'ThreadStudio',
        description:'Fashion Order Payment',
        order_id:order.orderId,

        handler:async function(response) {
          try {
            await api('/payments/verify', {
              method:'POST',
              body:JSON.stringify({
                razorpayOrderId:response.razorpay_order_id,
                razorpayPaymentId:response.razorpay_payment_id,
                razorpaySignature:response.razorpay_signature
              })
            });

            setCart([]);
            setAddress('');
            setPage('orders');
            flash('Payment successful! Order placed.');
          } catch(e) {
            flash(e.message || 'Payment verification failed');
          }
        },

        modal: {
          ondismiss: () => flash('Payment cancelled. Your cart is unchanged.')
        },

        theme: {
          color:'#d96b91'
        }
      };

      const razorpay = new window.Razorpay(options);
      razorpay.open();

    } catch(e) {
      flash(e.message || 'Checkout failed');
    }
  }

  async function createProduct(e) {
    e.preventDefault();
    try {
      await api('/designer/products', {
        method:'POST',
        body:JSON.stringify({...form,price:Number(form.price),stock:Number(form.stock)})
      });
      flash('Product submitted for review');
      setForm({title:'',description:'',price:'',category:'Contemporary',imageUrl:'',size:'S, M, L',stock:1});
      api('/designer/products').then(setMyProducts);
    } catch(e) {
      flash(e.message);
    }
  }

  async function adminAction(path) {
    try {
      await api(path,{method:'POST'});
      flash('Updated successfully');
      api('/admin/products').then(setAdminProducts);
      api('/admin/designers').then(setDesigners);
      loadProducts();
    } catch(e) {
      flash(e.message);
    }
  }

  const categories = ['All','Contemporary','Ethnic','Occasion','Minimal'];

  return (
    <div className="app">

      <div className="announcement">
        A new generation of fashion, made by emerging designers
        <span>✦</span> Discover independent labels
      </div>

      <header className="header">
        <button className="iconbtn mobile" onClick={() => setPage('shop')}>
          <Menu/>
        </button>

        <button className="wordmark" onClick={() => setPage('shop')}>
          thread<span>studio</span><i>.</i>
          <small>THE DESIGNER MARKETPLACE</small>
        </button>

        <nav>
          <button className={page === 'shop' ? 'active' : ''} onClick={() => setPage('shop')}>Shop</button>
          <button onClick={() => setPage('designer')}>Sell with us</button>
          <button onClick={() => setPage('orders')}>My orders</button>
        </nav>

        <div className="head-actions">
          <button className="iconbtn" aria-label="Account" onClick={() => setPage(token ? 'profile' : 'auth')}>
            <User/>
          </button>

          <button className="bag" onClick={() => setPage('cart')}>
            <ShoppingBag size={18}/>
            <span>Bag</span>
            <b>{cart.reduce((n,p) => n+p.qty,0)}</b>
          </button>
        </div>
      </header>

      {notice && <div className="toast">{notice}</div>}

      {page === 'shop' && <>
        <section className="hero">
          <div className="hero-copy">
            <span className="eyebrow">STYLE WITH A STORY</span>
            <h1>Wear a little<br/><em>originality.</em></h1>
            <p>Discover one-of-a-kind pieces created by the next generation of fashion designers.</p>
            <button className="darkbtn" onClick={() => document.getElementById('collection').scrollIntoView({behavior:'smooth'})}>
              Explore the collection <ArrowRight size={17}/>
            </button>
            <div className="hero-foot">
              <span>✳ Independent labels</span>
              <span>✳ Made with intention</span>
            </div>
          </div>

          <div className="hero-image">
            <img src="https://images.unsplash.com/photo-1539109136881-3be0616acf4b?auto=format&fit=crop&w=1300&q=90" alt="Fashion"/>
            <div className="image-tag">NEW SEASON <b>01 / 26</b></div>
          </div>
        </section>

        <section className="trust">
          <div><span>✳</span><p><b>Designed by students</b><small>Fresh ideas, original pieces</small></p></div>
          <div><span>♧</span><p><b>Independent labels</b><small>Support emerging talent</small></p></div>
          <div><span>♡</span><p><b>Made to be loved</b><small>Find your own kind of style</small></p></div>
          <div><span>↗</span><p><b>Direct from creators</b><small>Every piece has a story</small></p></div>
        </section>

        <section className="collection" id="collection">
          <div className="section-head">
            <div>
              <span className="eyebrow">THE EDIT</span>
              <h2>Pieces with <em>personality.</em></h2>
              <p>Thoughtfully made. Impossible to blend in.</p>
            </div>
            <div className="searchbox">
              <Search size={18}/>
              <input placeholder="Find your next favourite..." value={query} onChange={e => setQuery(e.target.value)}/>
            </div>
          </div>

          <div className="filters">
            {categories.map(c => <button key={c} className={category === c ? 'chosen' : ''} onClick={() => setCategory(c)}>{c}</button>)}
          </div>

          <div className="product-grid">
            {shown.map((p,i) => (
              <article className="product-card" key={p.id}>
                <div className="product-image" onClick={() => setSelected(p)}>
                  <img src={p.imageUrl || demo[i % demo.length].imageUrl} onError={e => e.currentTarget.src = demo[i % demo.length].imageUrl} alt={p.title}/>
                  <span className="product-label">{p.category || 'Designer pick'}</span>
                  <button className="heart" onClick={e => {e.stopPropagation();flash('Saved to your wishlist')}}><Heart size={18}/></button>
                  <button className="quick-add" onClick={e => {e.stopPropagation();add(p)}}><Plus size={17}/> Quick add</button>
                </div>
                <div className="product-meta">
                  <div><h3>{p.title}</h3><p>by {p.designer?.name || 'Independent designer'}</p></div>
                  <b>{money(p.price)}</b>
                </div>
              </article>
            ))}
          </div>
          {!shown.length && <div className="empty">No pieces found. Try another search.</div>}
        </section>

        <section className="designer-cta">
          <div>
            <span className="eyebrow">YOUR VISION. YOUR LABEL.</span>
            <h2>Make room for<br/><em>your kind of fashion.</em></h2>
            <p>Turn your sketches into a label people love. List your designs, build your brand and find your first customers.</p>
            <button className="lightbtn" onClick={() => {setAuthMode('register');setAuth({...auth,role:'DESIGNER'});setPage(token ? 'designer' : 'auth')}}>
              Become a designer <ArrowRight size={17}/>
            </button>
          </div>
          <img src="https://images.unsplash.com/photo-1558618666-fcd25c85cd64?auto=format&fit=crop&w=1100&q=85" alt="Designer"/>
        </section>
      </>}

      {page === 'auth' && <section className="panel auth-panel">
        <span className="eyebrow">WELCOME TO THREADSTUDIO</span>
        <h2>{authMode === 'login' ? 'Good to see you.' : 'Your story starts here.'}</h2>
        <p>Sign in or create an account to shop and sell independent fashion.</p>

        <form onSubmit={submitAuth}>
          {authMode === 'register' && <input required placeholder="Full name" value={auth.name} onChange={e => setAuth({...auth,name:e.target.value})}/>}
          <input required type="email" placeholder="Email address" value={auth.email} onChange={e => setAuth({...auth,email:e.target.value})}/>
          <input required minLength="8" type="password" placeholder="Password (8+ characters)" value={auth.password} onChange={e => setAuth({...auth,password:e.target.value})}/>

          {authMode === 'register' && <select value={auth.role} onChange={e => setAuth({...auth,role:e.target.value})}>
            <option value="USER">I want to shop</option>
            <option value="DESIGNER">I'm a fashion designer</option>
          </select>}

          <button className="darkbtn full">{authMode === 'login' ? 'Sign in' : 'Create account'} <ArrowRight size={17}/></button>
        </form>

        <p className="switch">
          {authMode === 'login' ? 'New to ThreadStudio?' : 'Already have an account?'}
          <button onClick={() => setAuthMode(authMode === 'login' ? 'register' : 'login')}>
            {authMode === 'login' ? 'Create account' : 'Sign in'}
          </button>
        </p>
      </section>}

      {/* CART SUMMARY: only payment options added */}
      {page === 'cart' && <section className="panel">
        <span className="eyebrow">YOUR SELECTION</span>
        <h2>Your bag <em>({cart.length})</em></h2>

        {cart.length ? <div className="cart-layout">
          <div>
            {cart.map(p => <div className="cart-row" key={p.id}>
              <img src={p.imageUrl} alt={p.title}/>
              <div className="cart-info">
                <b>{p.title}</b>
                <small>{p.designer?.name || 'Independent designer'}</small>
                <span>{money(p.price)} × {p.qty}</span>
              </div>
              <button className="iconbtn" onClick={() => setCart(cart.filter(x => x.id !== p.id))}>
                <Trash2 size={17}/>
              </button>
            </div>)}
          </div>

          <div className="summary">
            <h3>Order summary</h3>
            <p>Subtotal <b>{money(total)}</b></p>
            <p>Shipping <b>Calculated at checkout</b></p>
            <hr/>
            <p className="sum-total">Total <b>{money(total)}</b></p>

            <label>
              Shipping address
              <textarea value={address} onChange={e => setAddress(e.target.value)} placeholder="House, street, city, PIN code"/>
            </label>

            {/* New payment choices */}
            <div className="payment-method">
              <h4>Choose payment method</h4>

              <label className="payment-option">
                <input
                  type="radio"
                  name="paymentMethod"
                  value="COD"
                  checked={paymentMethod === 'COD'}
                  onChange={() => setPaymentMethod('COD')}
                />
                <span>
                  <b>Cash on Delivery</b>
                  <small>Pay when your order arrives.</small>
                </span>
              </label>

              <label className="payment-option">
                <input
                  type="radio"
                  name="paymentMethod"
                  value="RAZORPAY"
                  checked={paymentMethod === 'RAZORPAY'}
                  onChange={() => setPaymentMethod('RAZORPAY')}
                />
                <span>
                  <b>Pay Online</b>
                  <small>UPI, cards and other supported methods.</small>
                </span>
              </label>
            </div>

            <button className="darkbtn full" onClick={checkout}>
              {paymentMethod === 'COD' ? 'Place COD order' : 'Pay online'}
              <ArrowRight size={17}/>
            </button>

            <small>
              {paymentMethod === 'COD'
                ? 'Demo checkout: cash on delivery.'
                : 'Secure online payment through Razorpay.'}
            </small>
          </div>
        </div> : <div className="empty">
          <ShoppingBag size={32}/>
          <h3>Your bag is waiting for something lovely.</h3>
          <button className="darkbtn" onClick={() => setPage('shop')}>Explore designs</button>
        </div>}
      </section>}

      {page === 'orders' && <section className="panel">
        <span className="eyebrow">YOUR THREADSTUDIO</span>
        <h2>Order <em>history.</em></h2>

        {!token
          ? <button className="darkbtn" onClick={() => setPage('auth')}>Sign in to view orders</button>
          : orders.length
            ? orders.map(o => <div className="order-row" key={o.id}>
                <PackageCheck/>
                <div>
                  <b>Order #{o.id}</b>
                  <p>{o.items?.map(i => i.product?.title).join(', ')}</p>
                  <small>{o.status} · {o.paymentMethod?.replaceAll('_',' ')}</small>
                </div>
                <strong>{money(o.total)}</strong>
              </div>)
            : <div className="empty">No orders yet — your next favourite could be waiting in the collection.</div>
        }
      </section>}

      {page === 'profile' && <section className="panel">
        <span className="eyebrow">YOUR ACCOUNT</span>
        <h2>My <em>profile.</em></h2>

        {user ? <>
          <div className="order-row">
            <User/>
            <div>
              <b>{user.name}</b>
              <p>{user.email}</p>
              <small>Role: {user.role}</small>
            </div>
          </div>
          <button className="darkbtn" onClick={logout}><LogOut size={17}/> Logout</button>
        </> : <button className="darkbtn" onClick={() => setPage('auth')}>Sign in</button>}
      </section>}

      {page === 'designer' && <section className="panel">
        <span className="eyebrow">DESIGNER STUDIO</span>
        <h2>Bring your ideas <em>to life.</em></h2>

        {!token
          ? <button className="darkbtn" onClick={() => {setAuthMode('register');setAuth({...auth,role:'DESIGNER'});setPage('auth')}}>Join as a designer <ArrowRight size={17}/></button>
          : user?.role === 'USER'
            ? <div className="empty">Your account is a shopper account. Sign out and create a designer account to list your creations.</div>
            : <>
                <p>Submit original clothing designs. Admin approval is required before products appear publicly.</p>

                <form className="product-form" onSubmit={createProduct}>
                  <input required placeholder="Design name" value={form.title} onChange={e => setForm({...form,title:e.target.value})}/>
                  <textarea required placeholder="Describe your piece" value={form.description} onChange={e => setForm({...form,description:e.target.value})}/>

                  <div className="form-grid">
                    <input required type="number" min="1" placeholder="Price (₹)" value={form.price} onChange={e => setForm({...form,price:e.target.value})}/>
                    <select value={form.category} onChange={e => setForm({...form,category:e.target.value})}>
                      {categories.filter(x => x !== 'All').map(c => <option key={c}>{c}</option>)}
                    </select>
                    <input placeholder="Sizes e.g. S, M, L" value={form.size} onChange={e => setForm({...form,size:e.target.value})}/>
                    <input type="number" min="0" placeholder="Stock" value={form.stock} onChange={e => setForm({...form,stock:e.target.value})}/>
                  </div>

                  <input type="url" placeholder="Product image URL (https://...)" value={form.imageUrl} onChange={e => setForm({...form,imageUrl:e.target.value})}/>
                  <button className="darkbtn">Submit design for approval <ArrowRight size={17}/></button>
                </form>

                <h3 className="subhead">Your listings</h3>
                {myProducts.map(p => <div className="order-row" key={p.id}>
                  <Shirt/>
                  <div><b>{p.title}</b><p>{money(p.price)}</p></div>
                  <span className="status">{p.status}</span>
                </div>)}
              </>
        }
      </section>}

      {page === 'admin' && <section className="panel">
        <span className="eyebrow">CONTROL ROOM</span>
        <h2>Marketplace <em>review.</em></h2>

        {!token || user?.role !== 'ADMIN'
          ? <div className="empty">Admin access is required. Promote your first account to ADMIN in MySQL; see README.</div>
          : <>
              <h3 className="subhead">Designer applications</h3>
              {designers.map(d => <div className="order-row" key={d.id}>
                <User/>
                <div><b>{d.name}</b><p>{d.email}</p></div>
                <button className="smallbtn" onClick={() => adminAction(`/admin/designers/${d.id}/approve`)}>Approve designer</button>
              </div>)}

              <h3 className="subhead">Product moderation</h3>
              {adminProducts.map(p => <div className="order-row" key={p.id}>
                <img className="mini-img" src={p.imageUrl || demo[0].imageUrl}/>
                <div><b>{p.title}</b><p>{p.designer?.name} · {money(p.price)}</p><small>{p.status}</small></div>
                {p.status !== 'APPROVED' && <button className="smallbtn" onClick={() => adminAction(`/admin/products/${p.id}/approve`)}>Approve</button>}
                <button className="smallbtn reject" onClick={() => adminAction(`/admin/products/${p.id}/reject`)}>Reject</button>
              </div>)}
            </>
        }
      </section>}

      {selected && <div className="modal-backdrop" onClick={() => setSelected(null)}>
        <div className="modal" onClick={e => e.stopPropagation()}>
          <button className="modal-close" onClick={() => setSelected(null)}>×</button>
          <img src={selected.imageUrl}/>
          <div>
            <span className="eyebrow">{selected.category}</span>
            <h2>{selected.title}</h2>
            <p>Designed by {selected.designer?.name || 'an independent creator'}</p>
            <p>{selected.description}</p>
            <h3>{money(selected.price)}</h3>
            <button className="darkbtn full" onClick={() => {add(selected);setSelected(null)}}>Add to bag <ShoppingBag size={17}/></button>
          </div>
        </div>
      </div>}

      <footer>
        <div className="footer-brand">
          thread<span>studio</span><i>.</i>
          <p>Good clothes. Original ideas. A more creative world.</p>
        </div>

        <div>
          <b>Explore</b>
          <button onClick={() => setPage('shop')}>Shop all</button>
          <button onClick={() => setPage('designer')}>Sell your designs</button>
        </div>

        <div>
          <b>Your account</b>
          {token && user ? <>
            <button onClick={() => setPage('profile')}>My Profile</button>
            <button onClick={() => setPage('orders')}>Orders</button>
            <button onClick={logout}>Logout</button>
          </> : <button onClick={() => {setAuthMode('login');setPage('auth')}}>Sign in / Register</button>}
        </div>

        <div className="footer-note">
          Made for the next generation of fashion.
          <span>✳</span>
        </div>

        <small className="copyright">© 2026 ThreadStudio. Built to celebrate independent design.</small>
      </footer>

    </div>
  );
}