from fastapi import FastAPI
from sentence_transformers import SentenceTransformer
import faiss
import numpy as np

app = FastAPI()

# ✅ Step 1: Load model
model = SentenceTransformer('all-MiniLM-L6-v2')

# ✅ Step 2: Product data
products = [
    "Modern LED Bedroom Lamp warm light decor",
    "Study Table Lamp bright light work"
]

# ✅ Step 3: Create embeddings
embeddings = model.encode(products)

# ✅ Step 4: Create FAISS index
dimension = len(embeddings[0])
index = faiss.IndexFlatL2(dimension)
index.add(np.array(embeddings))


# ✅ Step 5: API
@app.get("/search")
def search(q: str):
    query_vector = model.encode([q])
    D, I = index.search(np.array(query_vector), k=2)
    return {
        "query": q,
        "results": I.tolist()
    }